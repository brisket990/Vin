import { IsString, MinLength } from 'class-validator';

/** Join an existing household with the CURRENT (already logged-in) account
 *  -- the counterpart to auth/dto/join-household.dto.ts, which instead
 *  creates a brand new account tied to that household. */
export class JoinHouseholdByCodeDto {
  @IsString()
  @MinLength(4)
  inviteCode!: string;
}
