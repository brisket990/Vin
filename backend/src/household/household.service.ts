import { ConflictException, Inject, Injectable, NotFoundException } from '@nestjs/common';
import { and, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { householdMembers, households, users } from '../db/schema.js';
import { generateInviteCode } from '../common/util/invite-code.js';

const MAX_INVITE_CODE_ATTEMPTS = 5;

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

    // Joined via household_members rather than users.householdId, since a
    // user's *default* household (that column) and the households they're
    // actually a member of can now differ -- see householdMembers.
    const members = await this.db
      .select({
        id: users.id,
        email: users.email,
        displayName: users.displayName,
        role: householdMembers.role,
      })
      .from(householdMembers)
      .innerJoin(users, eq(users.id, householdMembers.userId))
      .where(eq(householdMembers.householdId, householdId));

    return { ...household, members };
  }

  /** Every household the user belongs to (own or joined), most recently
   *  joined last -- used to render the foyer switcher. */
  async listMine(userId: string) {
    return this.db
      .select({
        id: households.id,
        name: households.name,
        inviteCode: households.inviteCode,
        role: householdMembers.role,
      })
      .from(householdMembers)
      .innerJoin(households, eq(households.id, householdMembers.householdId))
      .where(eq(householdMembers.userId, userId))
      .orderBy(householdMembers.joinedAt);
  }

  /** Creates an additional household for an already-logged-in user, who
   *  becomes its owner -- e.g. "Appartement" alongside their existing
   *  "Maison", each with its own members and invite code. */
  async createAdditional(userId: string, name: string) {
    return this.db.transaction(async (tx) => {
      let household: { id: string; name: string; inviteCode: string } | undefined;

      for (let attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
        try {
          [household] = await tx
            .insert(households)
            .values({ name, inviteCode: generateInviteCode() })
            .returning();
          break;
        } catch {
          if (attempt === MAX_INVITE_CODE_ATTEMPTS - 1) {
            throw new ConflictException(
              "Impossible de générer un code d'invitation, réessaie.",
            );
          }
        }
      }

      if (!household) {
        throw new ConflictException(
          "Impossible de générer un code d'invitation, réessaie.",
        );
      }

      await tx.insert(householdMembers).values({
        userId,
        householdId: household.id,
        role: 'owner',
      });

      return household;
    });
  }

  /** Adds an already-logged-in user to an existing household via its
   *  invite code -- the "join with my current account" counterpart to
   *  AuthService.joinHousehold (which creates a brand new account). */
  async joinByCode(userId: string, inviteCode: string) {
    const [household] = await this.db
      .select()
      .from(households)
      .where(eq(households.inviteCode, inviteCode.toUpperCase()))
      .limit(1);

    if (!household) {
      throw new NotFoundException("Code d'invitation invalide.");
    }

    const [existing] = await this.db
      .select({ id: householdMembers.id })
      .from(householdMembers)
      .where(
        and(eq(householdMembers.userId, userId), eq(householdMembers.householdId, household.id)),
      )
      .limit(1);

    if (existing) {
      throw new ConflictException('Tu es déjà membre de ce foyer.');
    }

    await this.db.insert(householdMembers).values({
      userId,
      householdId: household.id,
      role: 'member',
    });

    return household;
  }

  /** Confirms the user actually belongs to this household before switching
   *  a token to it -- returns their role there. */
  async assertMembership(userId: string, householdId: string) {
    const [membership] = await this.db
      .select({ role: householdMembers.role })
      .from(householdMembers)
      .where(
        and(eq(householdMembers.userId, userId), eq(householdMembers.householdId, householdId)),
      )
      .limit(1);

    if (!membership) {
      throw new NotFoundException("Tu n'es pas membre de ce foyer.");
    }
    return membership;
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
    for (let attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
      try {
        const [household] = await this.db
          .update(households)
          .set({ inviteCode: generateInviteCode() })
          .where(eq(households.id, householdId))
          .returning();
        return household;
      } catch {
        if (attempt === MAX_INVITE_CODE_ATTEMPTS - 1) throw new Error();
      }
    }
    throw new NotFoundException('Foyer introuvable.');
  }
}
