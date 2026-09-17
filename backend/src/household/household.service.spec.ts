import { afterAll, describe, expect, it } from 'vitest';
import { HouseholdService } from './household.service.js';
import { createTestDb, createTestHousehold } from '../test-utils/test-db.js';

describe('HouseholdService multi-household membership', () => {
  const { db, pool } = createTestDb();
  const householdService = new HouseholdService(db);

  afterAll(async () => {
    await pool.end();
  });

  it('lets an existing user create an additional household as its owner', async () => {
    const { user } = await createTestHousehold(db, 'Maison');

    const appart = await householdService.createAdditional(user.id, 'Appartement');

    const mine = await householdService.listMine(user.id);
    expect(mine.map((h) => h.id)).toContain(appart.id);
    expect(mine.find((h) => h.id === appart.id)?.role).toBe('owner');
    // The original household from createTestHousehold is still there too.
    expect(mine.length).toBeGreaterThanOrEqual(2);
  });

  it('lets a user join an existing household by invite code without creating a new account', async () => {
    const { household: appart } = await createTestHousehold(db, 'Appartement');
    const { user: roommate } = await createTestHousehold(db, 'Roommate default');

    const joined = await householdService.joinByCode(roommate.id, appart.inviteCode);
    expect(joined.id).toBe(appart.id);

    const mine = await householdService.listMine(roommate.id);
    expect(mine.map((h) => h.id)).toContain(appart.id);
    expect(mine.find((h) => h.id === appart.id)?.role).toBe('member');
  });

  it('refuses an invalid invite code', async () => {
    const { user } = await createTestHousehold(db, 'Maison');
    await expect(householdService.joinByCode(user.id, 'NOPE0000')).rejects.toThrow();
  });

  it('refuses joining a household the user already belongs to', async () => {
    const { household, user } = await createTestHousehold(db, 'Maison');
    await expect(householdService.joinByCode(user.id, household.inviteCode)).rejects.toThrow();
  });

  it('assertMembership resolves the role for a household the user belongs to, and rejects otherwise', async () => {
    const { household, user } = await createTestHousehold(db, 'Maison');
    const { household: other } = await createTestHousehold(db, 'Autre foyer');

    const membership = await householdService.assertMembership(user.id, household.id);
    expect(membership.role).toBe('owner');

    await expect(householdService.assertMembership(user.id, other.id)).rejects.toThrow();
  });

  it("lists every member of a household via household_members, not just users whose default household matches", async () => {
    const { household, user: owner } = await createTestHousehold(db, 'Foyer partagé');
    const { user: joiner } = await createTestHousehold(db, 'Autre foyer par défaut');
    await householdService.joinByCode(joiner.id, household.inviteCode);

    const withMembers = await householdService.getHousehold(household.id);
    const memberIds = withMembers.members.map((m) => m.id).sort();
    expect(memberIds).toEqual([owner.id, joiner.id].sort());
  });
});
