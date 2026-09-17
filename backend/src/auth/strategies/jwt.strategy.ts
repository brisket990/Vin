import { Inject, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { and, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../../db/drizzle.module.js';
import { householdMembers, users } from '../../db/schema.js';
import type { AuthenticatedUser, JwtPayload } from '../types.js';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    config: ConfigService,
    @Inject(DRIZZLE) private readonly db: DrizzleDb,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: config.getOrThrow<string>('JWT_SECRET'),
    });
  }

  async validate(payload: JwtPayload): Promise<AuthenticatedUser> {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, payload.sub))
      .limit(1);

    if (!user) {
      throw new UnauthorizedException('Utilisateur introuvable.');
    }

    // The token's householdId is which of the user's (possibly several)
    // households is active for THIS session -- see HouseholdService/
    // POST /household/switch. Trust it only once membership is confirmed
    // (it could be stale if the user was since removed from that household),
    // and read role from the membership row rather than the user's default,
    // since role can differ per household.
    const [membership] = await this.db
      .select({ role: householdMembers.role })
      .from(householdMembers)
      .where(
        and(
          eq(householdMembers.userId, user.id),
          eq(householdMembers.householdId, payload.householdId),
        ),
      )
      .limit(1);

    if (!membership) {
      throw new UnauthorizedException("Tu n'es plus membre de ce foyer.");
    }

    return {
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      householdId: payload.householdId,
      role: membership.role,
    };
  }
}
