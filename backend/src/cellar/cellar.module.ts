import { Module } from '@nestjs/common';
import { CellarService } from './cellar.service.js';
import { CellarController } from './cellar.controller.js';

@Module({
  providers: [CellarService],
  controllers: [CellarController],
  exports: [CellarService],
})
export class CellarModule {}
