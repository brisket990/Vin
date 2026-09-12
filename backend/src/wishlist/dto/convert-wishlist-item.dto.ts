import { IsIn, IsOptional, IsUUID } from 'class-validator';
import { wineColorValues } from '../../db/schema.js';

/**
 * Fields needed to turn a wishlist entry (a bottle you don't own yet) into a
 * real Bottle once you've bought it. Only `color` is required since a
 * wishlist item doesn't carry one.
 */
export class ConvertWishlistItemDto {
  @IsIn(wineColorValues)
  color!: (typeof wineColorValues)[number];

  @IsOptional()
  @IsUUID()
  locationId?: string;
}
