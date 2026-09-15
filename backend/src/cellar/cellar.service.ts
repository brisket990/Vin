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
   * Suggests up to 3 placements for `dto.quantity` bottles (default 1).
   *
   * Two rules drive the ranking, both aimed at keeping the cave tidy
   * instead of scattering bottles wherever there's a single free cell:
   *
   * 1. Grouping by style: within a row, a slot scores higher the more it's
   *    surrounded by bottles of the same color, and is actively penalised
   *    (not just under-scored) when the row is dominated by a *different*
   *    color -- so reds and whites cluster into their own areas rather
   *    than mixing.
   * 2. Best fit: candidates are runs of *contiguous* free slots within a
   *    single row (a real wine rack row, not wrapping to the next one).
   *    A run that fits the whole quantity is always preferred over one
   *    that doesn't, and among those, the smallest sufficient run wins --
   *    e.g. a lone 1-slot gap is used for a single bottle rather than
   *    breaking into a 3-slot gap, but two bottles bound for the same run
   *    go into that 3-slot gap together rather than being split up.
   *
   * Bottles nearing their drinking window still get a bonus for more
   * accessible (lower-numbered) rows, as before.
   */
  async suggestLocations(
    householdId: string,
    unitId: string,
    dto: SuggestLocationDto,
  ) {
    await this.getUnitOrThrow(householdId, unitId);
    const locations = await this.getLocationsWithOccupants(unitId);
    const quantity = dto.quantity ?? 1;

    const rows = new Map<number, LocationWithOccupant[]>();
    for (const location of locations) {
      const bucket = rows.get(location.row);
      if (bucket) bucket.push(location);
      else rows.set(location.row, [location]);
    }

    // Every maximal run of contiguous free slots within each row (rows are
    // already sorted by column from getLocationsWithOccupants).
    const runs: { row: number; locations: LocationWithOccupant[] }[] = [];
    for (const [row, rowLocations] of rows) {
      let current: LocationWithOccupant[] = [];
      for (const location of rowLocations) {
        if (!location.bottle) {
          current.push(location);
        } else if (current.length > 0) {
          runs.push({ row, locations: current });
          current = [];
        }
      }
      if (current.length > 0) runs.push({ row, locations: current });
    }

    if (runs.length === 0) return [];

    const currentYear = new Date().getFullYear();
    const isNearingPeak =
      dto.drinkUntilYear !== undefined && dto.drinkUntilYear - currentYear <= 2;

    const scored = runs.map((run) => {
      const rowOccupants = (rows.get(run.row) ?? []).filter((l) => l.bottle);
      const colorMatches = rowOccupants.filter((l) => l.bottle?.color === dto.color).length;
      const colorMismatches = rowOccupants.filter((l) => l.bottle?.color !== dto.color).length;
      const regionMatches = dto.region
        ? rowOccupants.filter((l) => l.bottle?.region === dto.region).length
        : 0;
      const accessibilityBonus = isNearingPeak ? -run.row : 0;
      const affinityScore =
        colorMatches * 10 - colorMismatches * 10 + regionMatches * 3 + accessibilityBonus;

      const fitsFully = run.locations.length >= quantity;
      const usableLength = Math.min(run.locations.length, quantity);
      const wasted = fitsFully ? run.locations.length - quantity : 0;

      return { run, affinityScore, fitsFully, usableLength, wasted };
    });

    scored.sort((a, b) => {
      if (a.fitsFully !== b.fitsFully) return a.fitsFully ? -1 : 1;
      if (a.affinityScore !== b.affinityScore) return b.affinityScore - a.affinityScore;
      if (a.fitsFully) {
        if (a.wasted !== b.wasted) return a.wasted - b.wasted;
      } else if (a.usableLength !== b.usableLength) {
        return b.usableLength - a.usableLength;
      }
      const aStart = a.run.locations[0];
      const bStart = b.run.locations[0];
      return aStart.row - bStart.row || aStart.column - bStart.column;
    });

    return scored.slice(0, 3).map(({ run, affinityScore, usableLength }) => {
      const start = run.locations[0];
      const end = run.locations[usableLength - 1];
      return {
        locationId: start.id,
        label: usableLength > 1 ? `${start.label} → ${end.label}` : start.label,
        row: start.row,
        column: start.column,
        score: affinityScore,
        runLength: usableLength,
      };
    });
  }

  /**
   * Given a starting slot and a count, walks the grid row-major (same row
   * left to right, then next row) starting at that slot and collects the
   * next free slots -- used when a scan/manual add covers several physical
   * bottles of the same wine, so each one gets its own casier instead of
   * stacking them all behind a single quantity count. May return fewer than
   * `count` entries if the unit runs out of free slots; the caller decides
   * what to do with any leftover quantity (e.g. leave it unassigned).
   */
  async findNextFreeLocations(
    householdId: string,
    startLocationId: string,
    count: number,
  ) {
    const [start] = await this.db
      .select({ unitId: cellarLocations.unitId })
      .from(cellarLocations)
      .innerJoin(cellarUnits, eq(cellarUnits.id, cellarLocations.unitId))
      .where(
        and(
          eq(cellarLocations.id, startLocationId),
          eq(cellarUnits.householdId, householdId),
        ),
      )
      .limit(1);

    if (!start) {
      throw new NotFoundException("Cet emplacement n'existe pas dans ta cave.");
    }

    const locations = await this.getLocationsWithOccupants(start.unitId);
    const startIndex = locations.findIndex((l) => l.id === startLocationId);
    if (startIndex === -1) {
      throw new NotFoundException("Cet emplacement n'existe pas dans ta cave.");
    }

    const result: { locationId: string; label: string; row: number; column: number }[] = [];
    for (let i = startIndex; i < locations.length && result.length < count; i++) {
      const location = locations[i];
      if (location.bottle) continue;
      result.push({
        locationId: location.id,
        label: location.label,
        row: location.row,
        column: location.column,
      });
    }
    return result;
  }
}
