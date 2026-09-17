import { afterAll, describe, expect, it } from 'vitest';
import { eq } from 'drizzle-orm';
import { BottleService, TURN_REMINDER_COOLDOWN_DAYS, TURN_REMINDER_THRESHOLD_DAYS } from './bottle.service.js';
import { bottles } from '../db/schema.js';
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

describe('BottleService quarter-turn reminder', () => {
  const { db, pool } = createTestDb();
  const bottleService = new BottleService(db);

  afterAll(async () => {
    await pool.end();
  });

  function daysAgo(days: number): Date {
    return new Date(Date.now() - days * 24 * 60 * 60 * 1000);
  }

  it('only flags a bottle as needing a turn once it has aged past the threshold', async () => {
    const { household } = await createTestHousehold(db, 'TurnAge');
    const fresh = await bottleService.create(household.id, { name: 'Jeune', color: 'red' });
    const old = await bottleService.create(household.id, { name: 'Ancienne', color: 'red' });
    await db
      .update(bottles)
      .set({ createdAt: daysAgo(TURN_REMINDER_THRESHOLD_DAYS + 1) })
      .where(eq(bottles.id, old.id));

    const needingTurn = await bottleService.findNeedingTurn(household.id);
    expect(needingTurn.map((b) => b.id)).toEqual([old.id]);
    expect(needingTurn.map((b) => b.id)).not.toContain(fresh.id);
  });

  it('resets the overdue baseline when a bottle is marked as turned', async () => {
    const { household } = await createTestHousehold(db, 'TurnReset');
    const bottle = await bottleService.create(household.id, { name: 'Vieux Rouge', color: 'red' });
    await db
      .update(bottles)
      .set({ createdAt: daysAgo(TURN_REMINDER_THRESHOLD_DAYS + 10) })
      .where(eq(bottles.id, bottle.id));

    expect((await bottleService.findNeedingTurn(household.id)).map((b) => b.id)).toContain(bottle.id);

    const turned = await bottleService.turn(household.id, bottle.id);
    expect(turned.lastTurnedAt).not.toBeNull();
    expect(turned.lastTurnReminderSentAt).toBeNull();

    expect(await bottleService.findNeedingTurn(household.id)).toHaveLength(0);
  });

  it('excludes an overdue bottle from the reminder query once reminded recently, but keeps it in the plain needing-turn list', async () => {
    const { household } = await createTestHousehold(db, 'TurnCooldown');
    const bottle = await bottleService.create(household.id, { name: 'Oubliée', color: 'white' });
    await db
      .update(bottles)
      .set({
        createdAt: daysAgo(TURN_REMINDER_THRESHOLD_DAYS + 30),
        lastTurnReminderSentAt: daysAgo(1),
      })
      .where(eq(bottles.id, bottle.id));

    expect((await bottleService.findNeedingTurn(household.id)).map((b) => b.id)).toContain(bottle.id);
    expect(
      (await bottleService.findNeedingTurnReminder(household.id)).map((b) => b.id),
    ).not.toContain(bottle.id);

    await db
      .update(bottles)
      .set({ lastTurnReminderSentAt: daysAgo(TURN_REMINDER_COOLDOWN_DAYS + 1) })
      .where(eq(bottles.id, bottle.id));

    expect(
      (await bottleService.findNeedingTurnReminder(household.id)).map((b) => b.id),
    ).toContain(bottle.id);
  });

  it('lists households needing a reminder and lets markTurnReminderSent silence them', async () => {
    const { household } = await createTestHousehold(db, 'TurnHouseholds');
    const bottle = await bottleService.create(household.id, { name: 'À signaler', color: 'red' });
    await db
      .update(bottles)
      .set({ createdAt: daysAgo(TURN_REMINDER_THRESHOLD_DAYS + 5) })
      .where(eq(bottles.id, bottle.id));

    expect(await bottleService.findHouseholdIdsNeedingTurnReminder()).toContain(household.id);

    await bottleService.markTurnReminderSent([bottle.id]);

    expect(await bottleService.findHouseholdIdsNeedingTurnReminder()).not.toContain(household.id);
  });
});

describe('BottleService apogée (drinking window) reminders', () => {
  const { db, pool } = createTestDb();
  const bottleService = new BottleService(db);
  const currentYear = new Date().getFullYear();

  afterAll(async () => {
    await pool.end();
  });

  it('only flags a bottle as entering its window once drinkFromYear is reached', async () => {
    const { household } = await createTestHousehold(db, 'ApogeeStartAge');
    const notYet = await bottleService.create(household.id, {
      name: 'Trop jeune',
      color: 'red',
      drinkFromYear: currentYear + 3,
    });
    const ready = await bottleService.create(household.id, {
      name: 'Prête',
      color: 'red',
      drinkFromYear: currentYear,
    });
    const noWindow = await bottleService.create(household.id, { name: 'Sans fenêtre', color: 'red' });

    const needing = await bottleService.findNeedingApogeeStartReminder(household.id);
    expect(needing.map((b) => b.id)).toEqual([ready.id]);
    expect(needing.map((b) => b.id)).not.toContain(notYet.id);
    expect(needing.map((b) => b.id)).not.toContain(noWindow.id);
  });

  it('only flags a bottle as finishing its window once drinkUntilYear is reached', async () => {
    const { household } = await createTestHousehold(db, 'ApogeeEndAge');
    const stillGood = await bottleService.create(household.id, {
      name: 'Encore bonne',
      color: 'red',
      drinkUntilYear: currentYear + 5,
    });
    const lastChance = await bottleService.create(household.id, {
      name: 'Dernière chance',
      color: 'red',
      drinkUntilYear: currentYear,
    });

    const needing = await bottleService.findNeedingApogeeEndReminder(household.id);
    expect(needing.map((b) => b.id)).toEqual([lastChance.id]);
    expect(needing.map((b) => b.id)).not.toContain(stillGood.id);
  });

  it('excludes a bottle whose one-time apogée reminder was already sent', async () => {
    const { household } = await createTestHousehold(db, 'ApogeeOnce');
    const bottle = await bottleService.create(household.id, {
      name: 'Déjà signalée',
      color: 'red',
      drinkFromYear: currentYear,
      drinkUntilYear: currentYear,
    });

    expect((await bottleService.findNeedingApogeeStartReminder(household.id)).map((b) => b.id)).toContain(
      bottle.id,
    );
    expect((await bottleService.findNeedingApogeeEndReminder(household.id)).map((b) => b.id)).toContain(
      bottle.id,
    );

    await bottleService.markApogeeStartReminderSent([bottle.id]);
    await bottleService.markApogeeEndReminderSent([bottle.id]);

    expect(
      (await bottleService.findNeedingApogeeStartReminder(household.id)).map((b) => b.id),
    ).not.toContain(bottle.id);
    expect(
      (await bottleService.findNeedingApogeeEndReminder(household.id)).map((b) => b.id),
    ).not.toContain(bottle.id);
  });

  it('lists households needing an apogée reminder and lets marking them sent silence them', async () => {
    const { household } = await createTestHousehold(db, 'ApogeeHouseholds');
    const bottle = await bottleService.create(household.id, {
      name: 'À signaler',
      color: 'white',
      drinkFromYear: currentYear,
    });

    expect(await bottleService.findHouseholdIdsNeedingApogeeStartReminder()).toContain(household.id);

    await bottleService.markApogeeStartReminderSent([bottle.id]);

    expect(await bottleService.findHouseholdIdsNeedingApogeeStartReminder()).not.toContain(household.id);
  });
});
