import { Module } from '@nestjs/common';
import { WishlistService } from './wishlist.service.js';
import { WishlistController } from './wishlist.controller.js';
import { BottleModule } from '../bottle/bottle.module.js';

@Module({
  imports: [BottleModule],
  providers: [WishlistService],
  controllers: [WishlistController],
})
export class WishlistModule {}
