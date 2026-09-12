import { Inject, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../../db/drizzle.module.js';
import { users } from '../../db/schema.js';
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

    return {
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      householdId: user.householdId,
      role: user.role,
    };
  }
}
