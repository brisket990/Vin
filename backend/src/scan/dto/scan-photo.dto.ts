import { IsIn, IsOptional } from 'class-validator';
import { aiProviderValues } from '../../db/schema.js';

export class ScanPhotoDto {
  @IsOptional()
  @IsIn(aiProviderValues)
  provider?: (typeof aiProviderValues)[number];
}
