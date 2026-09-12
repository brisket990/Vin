import { Module } from '@nestjs/common';
import { TastingService } from './tasting.service.js';
import { TastingController } from './tasting.controller.js';

@Module({
  providers: [TastingService],
  controllers: [TastingController],
})
export class TastingModule {}
