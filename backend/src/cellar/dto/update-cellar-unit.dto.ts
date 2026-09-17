import { IsIn, IsOptional, IsString, MinLength } from 'class-validator';
import { wineColorValues } from '../../db/schema.js';

/**
 * Rename a unit and/or change its dedicated color after creation. Deliberately
 * doesn't allow resizing (rowCount/columnCount) -- the grid's slots already
 * exist and may hold bottles, so changing dimensions would need a real
 * migration strategy for existing locations, which isn't asked for here.
 */
export class UpdateCellarUnitDto {
  @IsOptional()
  @IsString()
  @MinLength(1)
  name?: string;

  /** One of the wine colors to dedicate this unit to, or the literal string
   *  "none" to clear it back to "mixed" (no preference). Using a sentinel
   *  string rather than `null` here is deliberate: the Android client's JSON
   *  encoder omits null fields entirely, so a real `null` could never reach
   *  this endpoint to request a clear. Omitting the field altogether leaves
   *  the current value unchanged. */
  @IsOptional()
  @IsIn([...wineColorValues, 'none'])
  preferredColor?: (typeof wineColorValues)[number] | 'none';
}
