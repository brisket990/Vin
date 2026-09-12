import { afterAll, describe, expect, it } from 'vitest';
import { BottleService } from './bottle.service.js';
import { createTestDb, createTestHousehold } from '../test-utils/test-db.js';

describe('BottleService', () => {
  const { db, pool } = createTestDb();
  const bottleService = new BottleService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('filters by color, region and vintage range', async () => {
    const { household } = await createTestHousehold(db, 'Search');

    await bottleService.create(household.id, {
      name: 'Chablis',
      color: 'white',
      region: 'Bourgogne',
      vintage: 2020,
    });
    await bottleService.create(household.id, {
      name: 'Margaux',
      color: 'red',
      region: 'Bordeaux',
      vintage: 2015,
    });

    const reds = await bottleService.findAll(household.id, {});
    expect(reds).toHaveLength(2);

    const onlyWhite = await bottleService.findAll(household.id, { color: 'white' });
    expect(onlyWhite.map((b) => b.name)).toEqual(['Chablis']);

    const oldVintages = await bottleService.findAll(household.id, {
      vintageMax: 2018,
    });
    expect(oldVintages.map((b) => b.name)).toEqual(['Margaux']);

    const searchByName = await bottleService.findAll(household.id, {
      search: 'chab',
    });
    expect(searchByName.map((b) => b.name)).toEqual(['Chablis']);
  });

  it('decrements quantity on partial consume and keeps the bottle in cellar', async () => {
    const { household, user } = await createTestHousehold(db, 'Consume');
    const bottle = await bottleService.create(household.id, {
      name: 'Champagne',
      color: 'sparkling',
      quantity: 3,
    });

    const { bottle: afterFirst } = await bottleService.consume(
      household.id,
      bottle.id,
      user.id,
      { quantity: 1, rating: 4 },
    );

    expect(afterFirst.quantity).toBe(2);
    expect(afterFirst.status).toBe('in_cellar');
  });

  it('marks the bottle consumed and frees its slot once quantity reaches zero', async () => {
    const { household, user } = await createTestHousehold(db, 'ConsumeAll');
    const cellarLocationId = undefined; // no location for this test
    const bottle = await bottleService.create(household.id, {
      name: 'Dernière bouteille',
      color: 'red',
      quantity: 1,
      locationId: cellarLocationId,
    });

    const { bottle: after, tastingNote } = await bottleService.consume(
      household.id,
      bottle.id,
      user.id,
      { rating: 5, comment: 'Excellent' },
    );

    expect(after.quantity).toBe(0);
    expect(after.status).toBe('consumed');
    expect(after.locationId).toBeNull();
    expect(tastingNote.rating).toBe(5);
  });

  it('rejects consuming more than the available quantity', async () => {
    const { household, user } = await createTestHousehold(db, 'Overconsume');
    const bottle = await bottleService.create(household.id, {
      name: 'Petit stock',
      color: 'red',
      quantity: 1,
    });

    await expect(
      bottleService.consume(household.id, bottle.id, user.id, { quantity: 5 }),
    ).rejects.toThrow();
  });
});
