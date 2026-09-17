import { IsUUID } from 'class-validator';

export class SwitchHouseholdDto {
  @IsUUID()
  householdId!: string;
}
