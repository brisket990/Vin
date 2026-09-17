import { afterAll, describe, expect, it } from 'vitest';
import { BottleService } from '../bottle/bottle.service.js';
import { ImportService } from './import.service.js';
import { createTestDb, createTestHousehold } from '../test-utils/test-db.js';

const CSV_HEADER =
  'name,producer,region,appellation,grapeVarieties,vintage,color,quantity,purchasePriceCents,purchaseDate,drinkFromYear,drinkUntilYear,location,status,notes';

describe('ImportService', () => {
  const { db, pool } = createTestDb();
  const bottleService = new BottleService(db);
  const importService = new ImportService(bottleService);

  afterAll(async () => {
    await pool.end();
  });

  it('imports valid rows without assigning a cellar location', async () => {
    const { household } = await createTestHousehold(db, 'Import');
    const csv = [
      CSV_HEADER,
      'Chablis,Domaine X,Bourgogne,,,"2020",white,2,1500,2021-03-01,2022,2028,R1-C1,in_cellar,Belle acidité',
      'Margaux,,,,"Merlot; Cabernet Sauvignon",2015,red,,,,,,,in_cellar,',
    ].join('\n');

    const result = await importService.importCsv(household.id, csv);

    expect(result.imported).toBe(2);
    expect(result.errors).toHaveLength(0);
    expect(result.skippedConsumed).toBe(0);

    const bottles = await bottleService.findAll(household.id, {});
    expect(bottles.map((b) => b.name).sort()).toEqual(['Chablis', 'Margaux']);
    expect(bottles.every((b) => b.locationId === null)).toBe(true);

    const margaux = bottles.find((b) => b.name === 'Margaux');
    expect(margaux?.grapeVarieties).toEqual(['Merlot', 'Cabernet Sauvignon']);
  });

  it('skips rows marked consumed instead of importing them as in-cellar', async () => {
    const { household } = await createTestHousehold(db, 'Import consumed');
    const csv = [
      CSV_HEADER,
      'Vieux Bordeaux,,,,,2010,red,1,,,,,,consumed,',
      'Encore en cave,,,,,2019,red,1,,,,,,in_cellar,',
    ].join('\n');

    const result = await importService.importCsv(household.id, csv);

    expect(result.imported).toBe(1);
    expect(result.skippedConsumed).toBe(1);
    const bottles = await bottleService.findAll(household.id, {});
    expect(bottles.map((b) => b.name)).toEqual(['Encore en cave']);
  });

  it('reports a per-row error for invalid data without aborting the rest of the file', async () => {
    const { household } = await createTestHousehold(db, 'Import errors');
    const csv = [
      CSV_HEADER,
      'Sans couleur,,,,,,,,,,,,,,in_cellar,', // missing required "color"
      'Valide,,,,,,red,,,,,,,in_cellar,',
    ].join('\n');

    const result = await importService.importCsv(household.id, csv);

    expect(result.imported).toBe(1);
    expect(result.errors).toHaveLength(1);
    expect(result.errors[0].row).toBe(2);

    const bottles = await bottleService.findAll(household.id, {});
    expect(bottles.map((b) => b.name)).toEqual(['Valide']);
  });

  it('refuses a CSV missing a required column in its header', async () => {
    const { household } = await createTestHousehold(db, 'Import bad header');
    const csv = 'producer,region\nDomaine X,Bourgogne';

    const result = await importService.importCsv(household.id, csv);

    expect(result.imported).toBe(0);
    expect(result.errors).toHaveLength(1);
    expect(result.errors[0].message).toContain('name');
  });

  it('reports an empty file rather than throwing', async () => {
    const { household } = await createTestHousehold(db, 'Import empty');
    const result = await importService.importCsv(household.id, '');
    expect(result.imported).toBe(0);
    expect(result.errors).toHaveLength(1);
  });
});
