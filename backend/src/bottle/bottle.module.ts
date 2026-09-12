import { Module } from '@nestjs/common';
import { BottleService } from './bottle.service.js';
import { BottleController } from './bottle.controller.js';

@Module({
  providers: [BottleService],
  controllers: [BottleController],
  exports: [BottleService],
})
export class BottleModule {}
