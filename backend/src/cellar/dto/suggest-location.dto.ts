import { IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';
import { wineColorValues } from '../../db/schema.js';

export class SuggestLocationDto {
  @IsIn(wineColorValues)
  color!: (typeof wineColorValues)[number];

  @IsOptional()
  @IsString()
  region?: string;

  @IsOptional()
  @IsInt()
  @Min(1900)
  @Max(2200)
  drinkFromYear?: number;

  @IsOptional()
  @IsInt()
  @Min(1900)
  @Max(2200)
  drinkUntilYear?: number;
}
