import { IsEmail, IsString, MinLength } from 'class-validator';

export class JoinHouseholdDto {
  @IsString()
  @MinLength(4)
  inviteCode!: string;

  @IsEmail()
  email!: string;

  @IsString()
  @MinLength(8)
  password!: string;

  @IsString()
  @MinLength(1)
  displayName!: string;
}
