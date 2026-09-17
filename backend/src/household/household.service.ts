import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Inject,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { and, eq, ne } from 'drizzle-orm';
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

  /**
   * Permanently deletes a household -- only its owner can, only while no
   * other account is a member (deleting out from under someone else would
   * either orphan their account or silently drop their only foyer, neither
   * of which this offers a way to recover from), and never the caller's
   * last remaining household (the app always needs one active foyer).
   *
   * `users.householdId` has an ON DELETE CASCADE straight to `households`
   * (it's every user's *default* foyer at login) -- if this happens to be
   * the caller's default, deleting the row as-is would cascade-delete the
   * caller's own account. So this reassigns that column to a household the
   * caller still belongs to *before* deleting, inside the same transaction.
   *
   * Returns the caller's fallback household (id + role) so the controller
   * can reissue a token for it when the household just deleted was the
   * one the caller's current JWT was scoped to.
   */
  async deleteHousehold(userId: string, householdId: string) {
    const membership = await this.assertMembership(userId, householdId);
    if (membership.role !== 'owner') {
      throw new ForbiddenException('Seul le/la propriétaire peut supprimer ce foyer.');
    }

    const members = await this.db
      .select({ userId: householdMembers.userId })
      .from(householdMembers)
      .where(eq(householdMembers.householdId, householdId));
    if (members.some((m) => m.userId !== userId)) {
      throw new BadRequestException(
        "Ce foyer a d'autres membres -- ils doivent d'abord le quitter avant qu'il puisse être supprimé.",
      );
    }

    const otherMemberships = await this.db
      .select({ householdId: householdMembers.householdId, role: householdMembers.role })
      .from(householdMembers)
      .where(and(eq(householdMembers.userId, userId), ne(householdMembers.householdId, householdId)))
      .limit(1);
    const fallback = otherMemberships[0];
    if (!fallback) {
      throw new BadRequestException('Tu ne peux pas supprimer ton dernier foyer.');
    }

    await this.db.transaction(async (tx) => {
      const [currentUser] = await tx.select().from(users).where(eq(users.id, userId)).limit(1);
      if (currentUser?.householdId === householdId) {
        await tx
          .update(users)
          .set({ householdId: fallback.householdId, role: fallback.role })
          .where(eq(users.id, userId));
      }
      // household_members, cellar units/locations, bottles, scan results,
      // pairing suggestions, wishlist items and device tokens for this
      // household all cascade-delete automatically (ON DELETE CASCADE).
      await tx.delete(households).where(eq(households.id, householdId));
    });

    return fallback;
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
