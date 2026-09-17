import { IsIn, IsInt, IsOptional, IsString, Max, Min, MinLength } from 'class-validator';
import { wineColorValues } from '../../db/schema.js';

/**
 * Rename a unit, change its dedicated color, and/or resize its grid after
 * creation. Resizing is handled by CellarService.updateUnit: growing adds
 * the new slots, shrinking removes the slots that fall outside the new grid
 * -- refused (BadRequestException) if any of them still holds a bottle.
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

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(200)
  rowCount?: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(200)
  columnCount?: number;
}
