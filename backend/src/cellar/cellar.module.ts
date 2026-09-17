import { Module } from '@nestjs/common';
import { CellarService } from './cellar.service.js';
import { CellarController, CellarLocationsController } from './cellar.controller.js';

@Module({
  providers: [CellarService],
  controllers: [CellarController, CellarLocationsController],
  exports: [CellarService],
})
export class CellarModule {}
