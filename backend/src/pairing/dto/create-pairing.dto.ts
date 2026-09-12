import { IsIn, IsOptional, IsString, MinLength } from 'class-validator';
import { aiProviderValues } from '../../db/schema.js';

export class CreatePairingDto {
  @IsString()
  @MinLength(2)
  dishDescription!: string;

  @IsOptional()
  @IsIn(aiProviderValues)
  provider?: (typeof aiProviderValues)[number];
}
