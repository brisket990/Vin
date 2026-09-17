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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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

describe('CellarService.removeUnit', () => {
  const { db, pool } = createTestDb();
  const cellarService = new CellarService(db);
  const bottleService = new BottleService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('deletes an empty unit', async () => {
    const { household } = await createTestHousehold(db, 'RemoveEmpty');
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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

describe('CellarService sites', () => {
  const { db, pool } = createTestDb();
  const cellarService = new CellarService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('creates, lists and renames sites for a household', async () => {
    const { household } = await createTestHousehold(db, 'Sites');

    const maison = await cellarService.createSite(household.id, { name: 'Maison' });
    const appart = await cellarService.createSite(household.id, { name: 'Appartement' });

    const sites = await cellarService.listSites(household.id);
    expect(sites.map((s) => s.id).sort()).toEqual([maison.id, appart.id].sort());

    const renamed = await cellarService.updateSite(household.id, appart.id, {
      name: 'Appartement Paris',
    });
    expect(renamed.name).toBe('Appartement Paris');
  });

  it('rejects creating a unit under a site belonging to another household', async () => {
    const { household: householdA } = await createTestHousehold(db, 'SiteCrossA');
    const { household: householdB } = await createTestHousehold(db, 'SiteCrossB');
    const siteB = await cellarService.createSite(householdB.id, { name: 'Cave B' });

    await expect(
      cellarService.createUnit(householdA.id, {
        siteId: siteB.id,
        name: 'Intrus',
        rowCount: 1,
        columnCount: 1,
      }),
    ).rejects.toThrow();
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
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const reds = await cellarService.createUnit(household.id, {
      siteId: site.id,
      name: 'Cave rouges',
      rowCount: 1,
      columnCount: 2,
      preferredColor: 'red',
    });
    const whites = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
      siteId: site.id,
      color: 'red',
    });

    expect(suggestions.length).toBeGreaterThan(0);
    expect(suggestions[0].unitId).toBe(reds.id);
  });

  it('avoids a mismatched dedicated unit while a mixed unit still competes normally', async () => {
    const { household } = await createTestHousehold(db, 'AcrossUnitsMixed');
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const reds = await cellarService.createUnit(household.id, {
      siteId: site.id,
      name: 'Cave rouges',
      rowCount: 1,
      columnCount: 1,
      preferredColor: 'red',
    });
    const mixed = await cellarService.createUnit(household.id, {
      siteId: site.id,
      name: 'Cave mixte',
      rowCount: 1,
      columnCount: 1,
    });

    const suggestions = await cellarService.suggestAcrossUnits(household.id, {
      siteId: site.id,
      color: 'white',
    });

    expect(suggestions.length).toBeGreaterThan(0);
    // The "reds" unit is a worse fit for a white bottle than the neutral one.
    expect(suggestions[0].unitId).toBe(mixed.id);
    expect(suggestions.some((s) => s.unitId === reds.id)).toBe(true);
  });

  it('returns nothing when every unit is full', async () => {
    const { household } = await createTestHousehold(db, 'AcrossUnitsFull');
    const site = await cellarService.createSite(household.id, { name: 'Maison' });
    const unit = await cellarService.createUnit(household.id, {
      siteId: site.id,
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
      siteId: site.id,
      color: 'red',
    });
    expect(suggestions).toHaveLength(0);
  });

  it('never suggests a slot in a different site, even if that site has room', async () => {
    const { household } = await createTestHousehold(db, 'AcrossSites');
    const maison = await cellarService.createSite(household.id, { name: 'Maison' });
    const appart = await cellarService.createSite(household.id, { name: 'Appartement' });

    await cellarService.createUnit(household.id, {
      siteId: maison.id,
      name: 'Casier maison',
      rowCount: 1,
      columnCount: 1,
    });
    const appartUnit = await cellarService.createUnit(household.id, {
      siteId: appart.id,
      name: 'Casier appart',
      rowCount: 1,
      columnCount: 1,
    });

    const suggestions = await cellarService.suggestAcrossUnits(household.id, {
      siteId: appart.id,
      color: 'red',
    });

    expect(suggestions).toHaveLength(1);
    expect(suggestions[0].unitId).toBe(appartUnit.id);
  });

  it('requires a siteId', async () => {
    const { household } = await createTestHousehold(db, 'AcrossUnitsNoSite');
    await expect(
      cellarService.suggestAcrossUnits(household.id, { color: 'red' }),
    ).rejects.toThrow();
  });
});
