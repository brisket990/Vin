import {
  ConflictException,
  Inject,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { eq } from 'drizzle-orm';
import bcrypt from 'bcryptjs';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { householdMembers, households, users } from '../db/schema.js';
import { generateInviteCode } from '../common/util/invite-code.js';
import type { RegisterHouseholdDto } from './dto/register-household.dto.js';
import type { JoinHouseholdDto } from './dto/join-household.dto.js';
import type { LoginDto } from './dto/login.dto.js';
import type { AuthenticatedUser, JwtPayload } from './types.js';

const SALT_ROUNDS = 12;
const MAX_INVITE_CODE_ATTEMPTS = 5;

@Injectable()
export class AuthService {
  constructor(
    @Inject(DRIZZLE) private readonly db: DrizzleDb,
    private readonly jwtService: JwtService,
  ) {}

  private toAuthenticatedUser(user: {
    id: string;
    email: string;
    displayName: string;
    householdId: string;
    role: 'owner' | 'member';
  }): AuthenticatedUser {
    return {
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      householdId: user.householdId,
      role: user.role,
    };
  }

  private signToken(user: AuthenticatedUser): string {
    const payload: JwtPayload = {
      sub: user.id,
      householdId: user.householdId,
      role: user.role,
    };
    return this.jwtService.sign(payload);
  }

  /** Issues a token scoped to a specific household for an already-known
   *  user -- used by HouseholdService after switching the active household,
   *  creating an additional one, or joining one by invite code while
   *  already logged in. Trusts the caller to have already confirmed
   *  membership (see HouseholdService.assertMembership). */
  async issueTokenForHousehold(userId: string, householdId: string, role: 'owner' | 'member') {
    const [user] = await this.db.select().from(users).where(eq(users.id, userId)).limit(1);
    if (!user) {
      throw new NotFoundException('Utilisateur introuvable.');
    }

    const authenticatedUser: AuthenticatedUser = {
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      householdId,
      role,
    };
    return {
      accessToken: this.signToken(authenticatedUser),
      user: authenticatedUser,
    };
  }

  private async assertEmailAvailable(email: string) {
    const [existing] = await this.db
      .select({ id: users.id })
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    if (existing) {
      throw new ConflictException('Un compte existe déjà avec cet email.');
    }
  }

  async registerHousehold(dto: RegisterHouseholdDto) {
    await this.assertEmailAvailable(dto.email);
    const passwordHash = await bcrypt.hash(dto.password, SALT_ROUNDS);

    const user = await this.db.transaction(async (tx) => {
      let household: { id: string; name: string; inviteCode: string } | undefined;

      for (let attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
        try {
          [household] = await tx
            .insert(households)
            .values({
              name: dto.householdName,
              inviteCode: generateInviteCode(),
            })
            .returning();
          break;
        } catch {
          // Unique constraint collision on inviteCode: retry with a new code.
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

      const [createdUser] = await tx
        .insert(users)
        .values({
          email: dto.email,
          passwordHash,
          displayName: dto.displayName,
          householdId: household.id,
          role: 'owner',
        })
        .returning();

      await tx.insert(householdMembers).values({
        userId: createdUser.id,
        householdId: household.id,
        role: 'owner',
      });

      return createdUser;
    });

    const authenticatedUser = this.toAuthenticatedUser(user);
    return {
      accessToken: this.signToken(authenticatedUser),
      user: authenticatedUser,
    };
  }

  async joinHousehold(dto: JoinHouseholdDto) {
    const [household] = await this.db
      .select()
      .from(households)
      .where(eq(households.inviteCode, dto.inviteCode.toUpperCase()))
      .limit(1);

    if (!household) {
      throw new NotFoundException("Code d'invitation invalide.");
    }

    await this.assertEmailAvailable(dto.email);
    const passwordHash = await bcrypt.hash(dto.password, SALT_ROUNDS);

    const user = await this.db.transaction(async (tx) => {
      const [createdUser] = await tx
        .insert(users)
        .values({
          email: dto.email,
          passwordHash,
          displayName: dto.displayName,
          householdId: household.id,
          role: 'member',
        })
        .returning();

      await tx.insert(householdMembers).values({
        userId: createdUser.id,
        householdId: household.id,
        role: 'member',
      });

      return createdUser;
    });

    const authenticatedUser = this.toAuthenticatedUser(user);
    return {
      accessToken: this.signToken(authenticatedUser),
      user: authenticatedUser,
    };
  }

  async login(dto: LoginDto) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.email, dto.email))
      .limit(1);

    if (!user) {
      throw new UnauthorizedException('Identifiants invalides.');
    }

    const passwordValid = await bcrypt.compare(dto.password, user.passwordHash);
    if (!passwordValid) {
      throw new UnauthorizedException('Identifiants invalides.');
    }

    const authenticatedUser = this.toAuthenticatedUser(user);
    return {
      accessToken: this.signToken(authenticatedUser),
      user: authenticatedUser,
    };
  }
}
