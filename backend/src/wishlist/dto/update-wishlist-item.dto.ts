import { PartialType } from '@nestjs/mapped-types';
import { CreateWishlistItemDto } from './create-wishlist-item.dto.js';

export class UpdateWishlistItemDto extends PartialType(CreateWishlistItemDto) {}
