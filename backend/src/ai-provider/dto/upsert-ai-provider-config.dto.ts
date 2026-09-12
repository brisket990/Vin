import { IsBoolean, IsIn, IsOptional, IsString, MinLength } from 'class-validator';
import { aiProviderValues, aiUsageValues } from '../../db/schema.js';

export class UpsertAiProviderConfigDto {
  @IsIn(aiProviderValues)
  provider!: (typeof aiProviderValues)[number];

  @IsString()
  @MinLength(10)
  apiKey!: string;

  @IsOptional()
  @IsString()
  model?: string;

  @IsOptional()
  @IsIn(aiUsageValues)
  usage?: (typeof aiUsageValues)[number];

  @IsOptional()
  @IsBoolean()
  isDefault?: boolean;
}
