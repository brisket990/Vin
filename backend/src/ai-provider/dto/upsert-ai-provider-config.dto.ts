import { IsBoolean, IsIn, IsOptional, IsString, IsUrl, MinLength } from 'class-validator';
import { aiProviderValues, aiUsageValues } from '../../db/schema.js';

export class UpsertAiProviderConfigDto {
  @IsIn(aiProviderValues)
  provider!: (typeof aiProviderValues)[number];

  // Optional at the DTO level because 'ollama' (self-hosted, local network)
  // doesn't need one -- the service layer enforces it for every other
  // provider (see AiProviderConfigService.upsert).
  @IsOptional()
  @IsString()
  @MinLength(10)
  apiKey?: string;

  @IsOptional()
  @IsString()
  model?: string;

  // Only meaningful for 'ollama': the household's self-hosted server
  // address, e.g. http://192.168.1.50:11434 (no path suffix).
  @IsOptional()
  @IsUrl({ require_tld: false })
  baseUrl?: string;

  @IsOptional()
  @IsIn(aiUsageValues)
  usage?: (typeof aiUsageValues)[number];

  @IsOptional()
  @IsBoolean()
  isDefault?: boolean;
}
