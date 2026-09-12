import { IsEmail, IsString, MinLength } from 'class-validator';

export class RegisterHouseholdDto {
  @IsString()
  @MinLength(2)
  householdName!: string;

  @IsEmail()
  email!: string;

  @IsString()
  @MinLength(8)
  password!: string;

  @IsString()
  @MinLength(1)
  displayName!: string;
}
