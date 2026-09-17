import { drizzle } from 'drizzle-orm/node-postgres';
import { Pool } from 'pg';
import * as schema from '../db/schema.js';
import { generateInviteCode } from '../common/util/invite-code.js';

/**
 * Connects to the local Postgres instance used for development (see
 * docker-compose.yml). Tests that touch the DB are integration tests by
 * design -- the schema is small enough that mocking Drizzle's query builder
 * would cost more than it proves. Requires `docker compose up db` (or an
 * equivalent local Postgres) to be running with migrations applied.
 */
export function createTestDb() {
  const connectionString =
    process.env.DATABASE_URL ?? 'postgresql://vin:vin@localhost:5432/vin';
  const pool = new Pool({ connectionString });
  return { db: drizzle(pool, { schema }), pool };
}

export async function createTestHousehold(
  db: ReturnType<typeof createTestDb>['db'],
  namePrefix = 'Test',
) {
  const [household] = await db
    .insert(schema.households)
    .values({
      name: `${namePrefix} ${crypto.randomUUID()}`,
      inviteCode: generateInviteCode(),
    })
    .returning();

  const [user] = await db
    .insert(schema.users)
    .values({
      email: `${crypto.randomUUID()}@example.com`,
      passwordHash: 'not-a-real-hash',
      displayName: 'Test User',
      householdId: household.id,
      role: 'owner',
    })
    .returning();

  await db.insert(schema.householdMembers).values({
    userId: user.id,
    householdId: household.id,
    role: 'owner',
  });

  return { household, user };
}
