import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { WishlistService } from './wishlist.service.js';
import { CreateWishlistItemDto } from './dto/create-wishlist-item.dto.js';
import { UpdateWishlistItemDto } from './dto/update-wishlist-item.dto.js';
import { ConvertWishlistItemDto } from './dto/convert-wishlist-item.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('wishlist')
export class WishlistController {
  constructor(private readonly wishlistService: WishlistService) {}

  @Post()
  create(@CurrentUser() user: AuthenticatedUser, @Body() dto: CreateWishlistItemDto) {
    return this.wishlistService.create(user.householdId, dto);
  }

  @Get()
  findAll(@CurrentUser() user: AuthenticatedUser) {
    return this.wishlistService.findAll(user.householdId);
  }

  @Patch(':id')
  update(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: UpdateWishlistItemDto,
  ) {
    return this.wishlistService.update(user.householdId, id, dto);
  }

  @Delete(':id')
  remove(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.wishlistService.remove(user.householdId, id);
  }

  @Post(':id/convert-to-bottle')
  convert(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: ConvertWishlistItemDto,
  ) {
    return this.wishlistService.convertToBottle(user.householdId, id, dto);
  }
}
