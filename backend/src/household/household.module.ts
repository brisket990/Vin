import { Module } from '@nestjs/common';
import { HouseholdService } from './household.service.js';
import { HouseholdController } from './household.controller.js';

@Module({
  providers: [HouseholdService],
  controllers: [HouseholdController],
  exports: [HouseholdService],
})
export class HouseholdModule {}
