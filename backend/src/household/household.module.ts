import { Module } from '@nestjs/common';
import { HouseholdService } from './household.service.js';
import { HouseholdController } from './household.controller.js';
import { AuthModule } from '../auth/auth.module.js';

@Module({
  imports: [AuthModule],
  providers: [HouseholdService],
  controllers: [HouseholdController],
  exports: [HouseholdService],
})
export class HouseholdModule {}
