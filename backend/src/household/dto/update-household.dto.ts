import { IsString, MinLength } from 'class-validator';

export class UpdateHouseholdDto {
  @IsString()
  @MinLength(2)
  name!: string;
}
