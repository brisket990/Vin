import { Inject, Injectable, NotFoundException } from '@nestjs/common';
import { eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { households, users } from '../db/schema.js';
import { generateInviteCode } from '../common/util/invite-code.js';

@Injectable()
export class HouseholdService {
  constructor(@Inject(DRIZZLE) private readonly db: DrizzleDb) {}

  async getHousehold(householdId: string) {
    const [household] = await this.db
      .select()
      .from(households)
      .where(eq(households.id, householdId))
      .limit(1);

    if (!household) {
      throw new NotFoundException('Foyer introuvable.');
    }

    const members = await this.db
      .select({
        id: users.id,
        email: users.email,
        displayName: users.displayName,
        role: users.role,
      })
      .from(users)
      .where(eq(users.householdId, householdId));

    return { ...household, members };
  }

  async rename(householdId: string, name: string) {
    const [household] = await this.db
      .update(households)
      .set({ name })
      .where(eq(households.id, householdId))
      .returning();

    if (!household) {
      throw new NotFoundException('Foyer introuvable.');
    }
    return household;
  }

  async regenerateInviteCode(householdId: string) {
    for (let attempt = 0; attempt < 5; attempt++) {
      try {
        const [household] = await this.db
          .update(households)
          .set({ inviteCode: generateInviteCode() })
          .where(eq(households.id, householdId))
          .returning();
        return household;
      } catch {
        if (attempt === 4) throw new Error();
      }
    }
    throw new NotFoundException('Foyer introuvable.');
  }
}
