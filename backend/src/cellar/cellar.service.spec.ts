import { afterAll, describe, expect, it } from 'vitest';
import { CellarService } from './cellar.service.js';
import { BottleService } from '../bottle/bottle.service.js';
import { createTestDb, createTestHousehold } from '../test-utils/test-db.js';

describe('CellarService.suggestLocations', () => {
  const { db, pool } = createTestDb();
  const cellarService = new CellarService(db);
  const bottleService = new BottleService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('creates a full grid of labelled, unoccupied locations', async () => {
    const { household } = await createTestHousehold(db, 'Grid');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Cave test',
      rowCount: 2,
      columnCount: 2,
    });

    expect(unit.locations).toHaveLength(4);
    expect(unit.locations.every((l) => l.bottle === null)).toBe(true);
    expect(unit.locations.map((l) => l.label).sort()).toEqual([
      'R1-C1',
      'R1-C2',
      'R2-C1',
      'R2-C2',
    ]);
  });

  it('favours slots in a row that already holds the same color/region, and more accessible rows when the bottle nears its drinking window', async () => {
    const { household } = await createTestHousehold(db, 'Suggest');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Cave test',
      rowCount: 3,
      columnCount: 3,
    });

    const row1 = unit.locations.filter((l) => l.row === 1);
    await bottleService.create(household.id, {
      name: 'Bordeaux rouge',
      color: 'red',
      region: 'Bordeaux',
      locationId: row1[0].id,
    });

    const currentYear = new Date().getFullYear();
    const suggestions = await cellarService.suggestLocations(household.id, unit.id, {
      color: 'red',
      region: 'Bordeaux',
      drinkUntilYear: currentYear + 1, // nearing peak -> should favour low rows too
    });

    expect(suggestions.length).toBeGreaterThan(0);
    // The best suggestion should be the other free slot in row 1 (same color + region).
    expect(suggestions[0].row).toBe(1);
    // Scores should be sorted descending.
    for (let i = 1; i < suggestions.length; i++) {
      expect(suggestions[i - 1].score).toBeGreaterThanOrEqual(suggestions[i].score);
    }
  });

  it('best-fits a run of contiguous free slots to the requested quantity instead of splitting bottles up', async () => {
    const { household } = await createTestHousehold(db, 'BestFit');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Cave test',
      rowCount: 1,
      columnCount: 6,
    });

    const row1 = unit.locations.filter((l) => l.row === 1).sort((a, b) => a.column - b.column);
    // Layout: [free] [bottle] [free] [free] [free] [bottle]
    // -> a 1-slot gap at C1 and a 3-slot gap at C3-C5.
    await bottleService.create(household.id, { name: 'A', color: 'red', locationId: row1[1].id });
    await bottleService.create(household.id, { name: 'B', color: 'red', locationId: row1[5].id });

    const single = await cellarService.suggestLocations(household.id, unit.id, {
      color: 'red',
      quantity: 1,
    });
    // A single bottle goes into the small gap, not the big one.
    expect(single[0].locationId).toBe(row1[0].id);
    expect(single[0].runLength).toBe(1);

    const pair = await cellarService.suggestLocations(household.id, unit.id, {
      color: 'red',
      quantity: 2,
    });
    // Two bottles together go into the 3-slot gap (the 1-slot one can't fit them).
    expect(pair[0].locationId).toBe(row1[2].id);
    expect(pair[0].runLength).toBe(2);
  });

  it('never suggests an already-occupied slot', async () => {
    const { household } = await createTestHousehold(db, 'Occupied');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Petite cave',
      rowCount: 1,
      columnCount: 1,
    });

    await bottleService.create(household.id, {
      name: 'Seule bouteille',
      color: 'white',
      locationId: unit.locations[0].id,
    });

    const suggestions = await cellarService.suggestLocations(household.id, unit.id, {
      color: 'white',
    });

    expect(suggestions).toHaveLength(0);
  });
});

describe('CellarService.updateUnit', () => {
  const { db, pool } = createTestDb();
  const cellarService = new CellarService(db);
  const bottleService = new BottleService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('sets and clears a unit\'s preferred color', async () => {
    const { household } = await createTestHousehold(db, 'UpdateUnit');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Cave test',
      rowCount: 1,
      columnCount: 1,
    });
    expect(unit.preferredColor).toBeNull();

    const withColor = await cellarService.updateUnit(household.id, unit.id, {
      preferredColor: 'red',
    });
    expect(withColor.preferredColor).toBe('red');
    expect(withColor.name).toBe('Cave test');

    const renamed = await cellarService.updateUnit(household.id, unit.id, {
      name: 'Cave rouges',
    });
    // Untouched field survives a partial update.
    expect(renamed.preferredColor).toBe('red');
    expect(renamed.name).toBe('Cave rouges');

    const cleared = await cellarService.updateUnit(household.id, unit.id, {
      preferredColor: 'none',
    });
    expect(cleared.preferredColor).toBeNull();
  });

  it('grows a grid, adding only the new slots and keeping existing ones (and their bottles) untouched', async () => {
    const { household } = await createTestHousehold(db, 'GrowUnit');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Petite cave',
      rowCount: 2,
      columnCount: 2,
    });
    const original = unit.locations.find((l) => l.row === 1 && l.column === 1)!;
    await bottleService.create(household.id, {
      name: 'Toujours là',
      color: 'red',
      locationId: original.id,
    });

    const grown = await cellarService.updateUnit(household.id, unit.id, {
      rowCount: 3,
      columnCount: 4,
    });

    expect(grown.rowCount).toBe(3);
    expect(grown.columnCount).toBe(4);
    expect(grown.locations).toHaveLength(12);
    const stillThere = grown.locations.find((l) => l.id === original.id);
    expect(stillThere?.bottle?.color).toBe('red');
  });

  it('shrinks a grid, removing only the slots that fall outside the new size', async () => {
    const { household } = await createTestHousehold(db, 'ShrinkUnit');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Grande cave',
      rowCount: 3,
      columnCount: 3,
    });

    const shrunk = await cellarService.updateUnit(household.id, unit.id, {
      rowCount: 2,
      columnCount: 2,
    });

    expect(shrunk.rowCount).toBe(2);
    expect(shrunk.columnCount).toBe(2);
    expect(shrunk.locations).toHaveLength(4);
    expect(shrunk.locations.every((l) => l.row <= 2 && l.column <= 2)).toBe(true);
  });

  it('refuses to shrink a grid if that would remove a slot still holding a bottle', async () => {
    const { household } = await createTestHousehold(db, 'ShrinkOccupied');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Cave test',
      rowCount: 2,
      columnCount: 2,
    });
    const doomed = unit.locations.find((l) => l.row === 2 && l.column === 2)!;
    await bottleService.create(household.id, {
      name: 'Coincée',
      color: 'red',
      locationId: doomed.id,
    });

    await expect(
      cellarService.updateUnit(household.id, unit.id, { rowCount: 1, columnCount: 1 }),
    ).rejects.toThrow();

    // Nothing changed.
    const unchanged = await cellarService.getUnit(household.id, unit.id);
    expect(unchanged.rowCount).toBe(2);
    expect(unchanged.locations).toHaveLength(4);
  });
});

describe('CellarService.removeUnit', () => {
  const { db, pool } = createTestDb();
  const cellarService = new CellarService(db);
  const bottleService = new BottleService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('deletes an empty unit', async () => {
    const { household } = await createTestHousehold(db, 'RemoveEmpty');
    const unit = await cellarService.createUnit(household.id, {
      name: 'À supprimer',
      rowCount: 1,
      columnCount: 1,
    });

    await cellarService.removeUnit(household.id, unit.id);

    const units = await cellarService.listUnits(household.id);
    expect(units.map((u) => u.id)).not.toContain(unit.id);
  });

  it('refuses to delete a unit that still holds an in-cellar bottle', async () => {
    const { household } = await createTestHousehold(db, 'RemoveOccupied');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Occupé',
      rowCount: 1,
      columnCount: 1,
    });
    await bottleService.create(household.id, {
      name: 'Bouteille en place',
      color: 'red',
      locationId: unit.locations[0].id,
    });

    await expect(cellarService.removeUnit(household.id, unit.id)).rejects.toThrow();

    const units = await cellarService.listUnits(household.id);
    expect(units.map((u) => u.id)).toContain(unit.id);
  });

  it('allows deleting a unit once its bottle has been consumed', async () => {
    const { household, user } = await createTestHousehold(db, 'RemoveAfterConsume');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Bientôt vide',
      rowCount: 1,
      columnCount: 1,
    });
    const bottle = await bottleService.create(household.id, {
      name: 'À boire',
      color: 'red',
      locationId: unit.locations[0].id,
    });
    await bottleService.consume(household.id, bottle.id, user.id, {});

    await cellarService.removeUnit(household.id, unit.id);

    const units = await cellarService.listUnits(household.id);
    expect(units.map((u) => u.id)).not.toContain(unit.id);
  });
});

describe('CellarService.suggestAcrossUnits', () => {
  const { db, pool } = createTestDb();
  const cellarService = new CellarService(db);
  const bottleService = new BottleService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('strongly favours a unit whose preferredColor matches, even over better in-row affinity elsewhere', async () => {
    const { household } = await createTestHousehold(db, 'AcrossUnits');
    const reds = await cellarService.createUnit(household.id, {
      name: 'Cave rouges',
      rowCount: 1,
      columnCount: 2,
      preferredColor: 'red',
    });
    const whites = await cellarService.createUnit(household.id, {
      name: 'Cave blancs',
      rowCount: 1,
      columnCount: 2,
      preferredColor: 'white',
    });

    // Fill one slot of "Cave blancs" with a white bottle so, ignoring the
    // dedicated-color rule, its remaining free slot would score higher for
    // a white bottle (same-color row affinity) than anything in "Cave
    // rouges" (which holds nothing at all).
    await bottleService.create(household.id, {
      name: 'Chablis',
      color: 'white',
      locationId: whites.locations[0].id,
    });

    const suggestions = await cellarService.suggestAcrossUnits(household.id, {
      color: 'red',
    });

    expect(suggestions.length).toBeGreaterThan(0);
    expect(suggestions[0].unitId).toBe(reds.id);
  });

  it('avoids a mismatched dedicated unit while a mixed unit still competes normally', async () => {
    const { household } = await createTestHousehold(db, 'AcrossUnitsMixed');
    const reds = await cellarService.createUnit(household.id, {
      name: 'Cave rouges',
      rowCount: 1,
      columnCount: 1,
      preferredColor: 'red',
    });
    const mixed = await cellarService.createUnit(household.id, {
      name: 'Cave mixte',
      rowCount: 1,
      columnCount: 1,
    });

    const suggestions = await cellarService.suggestAcrossUnits(household.id, {
      color: 'white',
    });

    expect(suggestions.length).toBeGreaterThan(0);
    // The "reds" unit is a worse fit for a white bottle than the neutral one.
    expect(suggestions[0].unitId).toBe(mixed.id);
    expect(suggestions.some((s) => s.unitId === reds.id)).toBe(true);
  });

  it('returns nothing when every unit is full', async () => {
    const { household } = await createTestHousehold(db, 'AcrossUnitsFull');
    const unit = await cellarService.createUnit(household.id, {
      name: 'Petite cave',
      rowCount: 1,
      columnCount: 1,
    });
    await bottleService.create(household.id, {
      name: 'Seule bouteille',
      color: 'red',
      locationId: unit.locations[0].id,
    });

    const suggestions = await cellarService.suggestAcrossUnits(household.id, {
      color: 'red',
    });
    expect(suggestions).toHaveLength(0);
  });
});
