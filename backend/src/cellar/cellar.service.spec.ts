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
