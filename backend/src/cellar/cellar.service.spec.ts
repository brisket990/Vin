import { afterAll, beforeAll, describe, expect, it } from 'vitest';
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

    const suggestions = await cellarService.suggestAcrossUnits(household.id, { color: 'red' });

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

    const suggestions = await cellarService.suggestAcrossUnits(household.id, { color: 'white' });

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

    const suggestions = await cellarService.suggestAcrossUnits(household.id, { color: 'red' });
    expect(suggestions).toHaveLength(0);
  });
});
