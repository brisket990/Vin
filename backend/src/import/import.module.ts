import { Module } from '@nestjs/common';
import { ImportService } from './import.service.js';
import { ImportController } from './import.controller.js';
import { BottleModule } from '../bottle/bottle.module.js';

@Module({
  imports: [BottleModule],
  providers: [ImportService],
  controllers: [ImportController],
})
export class ImportModule {}
