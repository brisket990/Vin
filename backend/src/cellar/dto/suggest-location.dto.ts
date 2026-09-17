import { IsIn, IsInt, IsOptional, IsString, IsUUID, Max, Min } from 'class-validator';
import { wineColorValues } from '../../db/schema.js';

export class SuggestLocationDto {
  /** Required for the cross-unit suggestion (POST /cellar/units/suggest-location)
   *  -- restricts the search to that cave's units, since a slot in another
   *  cave (e.g. a different apartment) isn't useful when adding a bottle in
   *  person. Ignored by the single-unit route, which already knows its unit. */
  @IsOptional()
  @IsUUID()
  siteId?: string;

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
