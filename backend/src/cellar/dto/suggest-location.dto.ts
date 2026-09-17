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

  /** How many bottles need a spot -- lets the suggestion pick a
   *  right-sized run of contiguous free slots (best fit) instead of a
   *  single cell, so several bottles of the same wine end up next to
   *  each other rather than scattered. Defaults to 1. */
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(200)
  quantity?: number;
}
