import { Inject, Injectable, NotFoundException } from '@nestjs/common';
import { and, eq, inArray } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, cellarLocations, cellarUnits } from '../db/schema.js';
import type { CreateCellarUnitDto } from './dto/create-cellar-unit.dto.js';
import type { SuggestLocationDto } from './dto/suggest-location.dto.js';

export interface LocationWithOccupant {
  id: string;
  row: number;
  column: number;
  label: string;
  bottle: {
    id: string;
    color: string;
    region: string | null;
  } | null;
}

@Injectable()
export class CellarService {
  constructor(@Inject(DRIZZLE) private readonly db: DrizzleDb) {}

  async listUnits(householdId: string) {
    const units = await this.db
      .select()
      .from(cellarUnits)
      .where(eq(cellarUnits.householdId, householdId));

    return Promise.all(
      units.map(async (unit) => ({
        ...unit,
        locations: await this.getLocationsWithOccupants(unit.id),
      })),
    );
  }

  private async getUnitOrThrow(householdId: string, unitId: string) {
    const [unit] = await this.db
      .select()
      .from(cellarUnits)
      .where(
        and(eq(cellarUnits.id, unitId), eq(cellarUnits.householdId, householdId)),
      )
      .limit(1);

    if (!unit) throw new NotFoundException('Casier de cave introuvable.');
    return unit;
  }

  async getUnit(householdId: string, unitId: string) {
    const unit = await this.getUnitOrThrow(householdId, unitId);
    return { ...unit, locations: await this.getLocationsWithOccupants(unitId) };
  }

  private async getLocationsWithOccupants(
    unitId: string,
  ): Promise<LocationWithOccupant[]> {
    const locations = await this.db
      .select()
      .from(cellarLocations)
      .where(eq(cellarLocations.unitId, unitId));

    if (locations.length === 0) return [];

    const locationIds = locations.map((l) => l.id);
    const occupants = await this.db
      .select({
        id: bottles.id,
        color: bottles.color,
        region: bottles.region,
        locationId: bottles.locationId,
      })
      .from(bottles)
      .where(
        and(
          inArray(bottles.locationId, locationIds),
          eq(bottles.status, 'in_cellar'),
        ),
      );

    const occupantByLocation = new Map(
      occupants.map((o) => [o.locationId as string, o]),
    );

    return locations
      .map((location) => ({
        id: location.id,
        row: location.row,
        column: location.column,
        label: location.label,
        bottle: occupantByLocation.get(location.id) ?? null,
      }))
      .sort((a, b) => a.row - b.row || a.column - b.column);
  }

  /**
   * Creates a cellar unit and eagerly generates every slot of its grid
   * (rowCount x columnCount), labelled "R{row}-C{column}".
   */
  async createUnit(householdId: string, dto: CreateCellarUnitDto) {
    const unit = await this.db.transaction(async (tx) => {
      const [createdUnit] = await tx
        .insert(cellarUnits)
        .values({
          householdId,
          name: dto.name,
          rowCount: dto.rowCount,
          columnCount: dto.columnCount,
        })
        .returning();

      const slots = [];
      for (let row = 1; row <= dto.rowCount; row++) {
        for (let column = 1; column <= dto.columnCount; column++) {
          slots.push({
            unitId: createdUnit.id,
            row,
            column,
            label: `R${row}-C${column}`,
          });
        }
      }
      await tx.insert(cellarLocations).values(slots);

      return createdUnit;
    });

    return this.getUnit(householdId, unit.id);
  }

  /**
   * Suggests up to 3 free slots for a bottle, grouping by wine color/region
   * already present in the same row and favouring more accessible rows
   * (lower row number) for bottles nearing their drinking window.
   */
  async suggestLocations(
    householdId: string,
    unitId: string,
    dto: SuggestLocationDto,
  ) {
    await this.getUnitOrThrow(householdId, unitId);
    const locations = await this.getLocationsWithOccupants(unitId);

    const free = locations.filter((l) => !l.bottle);
    if (free.length === 0) return [];

    const currentYear = new Date().getFullYear();
    const isNearingPeak =
      dto.drinkUntilYear !== undefined && dto.drinkUntilYear - currentYear <= 2;

    const scored = free.map((location) => {
      const sameRow = locations.filter(
        (l) => l.row === location.row && l.bottle,
      );
      const colorMatches = sameRow.filter(
        (l) => l.bottle?.color === dto.color,
      ).length;
      const regionMatches = dto.region
        ? sameRow.filter((l) => l.bottle?.region === dto.region).length
        : 0;
      const accessibilityBonus = isNearingPeak ? -location.row : 0;

      const score = colorMatches * 10 + regionMatches * 3 + accessibilityBonus;
      return { location, score };
    });

    scored.sort(
      (a, b) =>
        b.score - a.score ||
        a.location.row - b.location.row ||
        a.location.column - b.location.column,
    );

    return scored.slice(0, 3).map((s) => ({
      locationId: s.location.id,
      label: s.location.label,
      row: s.location.row,
      column: s.location.column,
      score: s.score,
    }));
  }
}
