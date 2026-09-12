import { Type } from 'class-transformer';
import { IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';
import { bottleStatusValues, wineColorValues } from '../../db/schema.js';

export class QueryBottlesDto {
  @IsOptional()
  @IsIn(wineColorValues)
  color?: (typeof wineColorValues)[number];

  @IsOptional()
  @IsIn(bottleStatusValues)
  status?: (typeof bottleStatusValues)[number];

  @IsOptional()
  @IsString()
  region?: string;

  @IsOptional()
  @IsString()
  grape?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1900)
  @Max(2200)
  vintageMin?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1900)
  @Max(2200)
  vintageMax?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  priceMaxCents?: number;

  @IsOptional()
  @IsString()
  search?: string;
}
