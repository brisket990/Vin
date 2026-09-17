import { Inject, Injectable, NotFoundException } from '@nestjs/common';
import { and, eq, inArray } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, cellarLocations, cellarUnits } from '../db/schema.js';
import type { CreateCellarUnitDto } from './dto/create-cellar-unit.dto.js';
import type { UpdateCellarUnitDto } from './dto/update-cellar-unit.dto.js';
import type { SuggestLocationDto } from './dto/suggest-location.dto.js';

interface Run {
  row: number;
  locations: LocationWithOccupant[];
}

interface ScoredRun {
  run: Run;
  affinityScore: number;
  fitsFully: boolean;
  usableLength: number;
  wasted: number;
}

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
          preferredColor: dto.preferredColor ?? null,
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

  /** Renames a unit and/or changes its dedicated color (see the schema
   *  comment on `preferredColor`); dimensions are immutable once created. */
  async updateUnit(householdId: string, unitId: string, dto: UpdateCellarUnitDto) {
    await this.getUnitOrThrow(householdId, unitId);

    const updates: Partial<typeof cellarUnits.$inferInsert> = {};
    if (dto.name !== undefined) updates.name = dto.name;
    if (dto.preferredColor !== undefined) {
      updates.preferredColor = dto.preferredColor === 'none' ? null : dto.preferredColor;
    }

    if (Object.keys(updates).length > 0) {
      await this.db.update(cellarUnits).set(updates).where(eq(cellarUnits.id, unitId));
    }

    return this.getUnit(householdId, unitId);
  }

  /** Every maximal run of contiguous free slots within each row of `locations`
   *  (rows are already sorted by column from getLocationsWithOccupants). */
  private computeRuns(locations: LocationWithOccupant[]): Run[] {
    const rows = new Map<number, LocationWithOccupant[]>();
    for (const location of locations) {
      const bucket = rows.get(location.row);
      if (bucket) bucket.push(location);
      else rows.set(location.row, [location]);
    }

    const runs: Run[] = [];
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
    return runs;
  }

  /**
   * Scores each run for `dto`, driven by the same two rules regardless of
   * whether this is a single-unit or cross-unit search:
   *
   * 1. Grouping by style: within a row, a slot scores higher the more it's
   *    surrounded by bottles of the same color, and is actively penalised
   *    (not just under-scored) when the row is dominated by a *different*
   *    color -- so reds and whites cluster into their own areas rather
   *    than mixing.
   * 2. Best fit: a run that fits the whole quantity is always preferred
   *    over one that doesn't, and among those, the smallest sufficient run
   *    wins -- e.g. a lone 1-slot gap is used for a single bottle rather
   *    than breaking into a 3-slot gap, but two bottles bound for the same
   *    run go into that 3-slot gap together rather than being split up.
   *
   * Bottles nearing their drinking window still get a bonus for more
   * accessible (lower-numbered) rows. `unitAffinityBonus` layers a unit-level
   * preference on top (see suggestAcrossUnits) without affecting the
   * single-unit case, which always passes 0.
   */
  private scoreRuns(
    locations: LocationWithOccupant[],
    runs: Run[],
    dto: SuggestLocationDto,
    unitAffinityBonus = 0,
  ): ScoredRun[] {
    const rows = new Map<number, LocationWithOccupant[]>();
    for (const location of locations) {
      const bucket = rows.get(location.row);
      if (bucket) bucket.push(location);
      else rows.set(location.row, [location]);
    }

    const quantity = dto.quantity ?? 1;
    const currentYear = new Date().getFullYear();
    const isNearingPeak =
      dto.drinkUntilYear !== undefined && dto.drinkUntilYear - currentYear <= 2;

    return runs.map((run) => {
      const rowOccupants = (rows.get(run.row) ?? []).filter((l) => l.bottle);
      const colorMatches = rowOccupants.filter((l) => l.bottle?.color === dto.color).length;
      const colorMismatches = rowOccupants.filter((l) => l.bottle?.color !== dto.color).length;
      const regionMatches = dto.region
        ? rowOccupants.filter((l) => l.bottle?.region === dto.region).length
        : 0;
      const accessibilityBonus = isNearingPeak ? -run.row : 0;
      const affinityScore =
        colorMatches * 10 -
        colorMismatches * 10 +
        regionMatches * 3 +
        accessibilityBonus +
        unitAffinityBonus;

      const fitsFully = run.locations.length >= quantity;
      const usableLength = Math.min(run.locations.length, quantity);
      const wasted = fitsFully ? run.locations.length - quantity : 0;

      return { run, affinityScore, fitsFully, usableLength, wasted };
    });
  }

  private rankScored<T extends ScoredRun>(scored: T[]): T[] {
    return [...scored].sort((a, b) => {
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
  }

  /** Suggests up to 3 placements for `dto.quantity` bottles (default 1)
   *  within a single, already-known unit -- see scoreRuns for the ranking
   *  rules. Kept alongside suggestAcrossUnits (used when the household has
   *  more than one unit) for callers that already know which unit they want. */
  async suggestLocations(
    householdId: string,
    unitId: string,
    dto: SuggestLocationDto,
  ) {
    await this.getUnitOrThrow(householdId, unitId);
    const locations = await this.getLocationsWithOccupants(unitId);
    const runs = this.computeRuns(locations);
    if (runs.length === 0) return [];

    const scored = this.rankScored(this.scoreRuns(locations, runs, dto));

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

  /** How strongly a unit's dedicated `preferredColor` (see the schema
   *  comment) sways the cross-unit ranking below -- large enough to dominate
   *  the in-row affinity score (which tops out around a few dozen) so a
   *  household's explicit "this unit is for reds" choice wins over a merely
   *  plausible spot elsewhere, but not so absolute that a mismatched unit
   *  becomes literally unusable when every other unit is full. */
  private static readonly PREFERRED_COLOR_BONUS = 500;

  /**
   * Same placement logic as suggestLocations, but searches every cellar unit
   * of the household at once and returns the best 3 slots overall -- used
   * once a household has more than one unit, so the caller doesn't have to
   * pick a unit before asking where a bottle should go. A unit with a
   * `preferredColor` set is strongly favored when it matches `dto.color` and
   * strongly avoided when it doesn't (see PREFERRED_COLOR_BONUS); a unit
   * left "mixed" (no preferredColor) is ranked purely on in-row affinity,
   * exactly as suggestLocations does for a single unit.
   */
  async suggestAcrossUnits(householdId: string, dto: SuggestLocationDto) {
    const units = await this.db
      .select()
      .from(cellarUnits)
      .where(eq(cellarUnits.householdId, householdId));

    if (units.length === 0) return [];

    const candidates: (ScoredRun & { unitId: string; unitName: string })[] = [];

    for (const unit of units) {
      const locations = await this.getLocationsWithOccupants(unit.id);
      const runs = this.computeRuns(locations);
      if (runs.length === 0) continue;

      const unitAffinityBonus =
        unit.preferredColor == null
          ? 0
          : unit.preferredColor === dto.color
            ? CellarService.PREFERRED_COLOR_BONUS
            : -CellarService.PREFERRED_COLOR_BONUS;

      for (const scored of this.scoreRuns(locations, runs, dto, unitAffinityBonus)) {
        candidates.push({ ...scored, unitId: unit.id, unitName: unit.name });
      }
    }

    const ranked = this.rankScored(candidates);

    return ranked.slice(0, 3).map(({ unitId, unitName, run, affinityScore, usableLength }) => {
      const start = run.locations[0];
      const end = run.locations[usableLength - 1];
      return {
        unitId,
        unitName,
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
