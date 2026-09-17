import { Module } from '@nestjs/common';
import { CellarService } from './cellar.service.js';
import {
  CellarController,
  CellarLocationsController,
  CellarSitesController,
} from './cellar.controller.js';

@Module({
  providers: [CellarService],
  controllers: [CellarSitesController, CellarController, CellarLocationsController],
  exports: [CellarService],
})
export class CellarModule {}
