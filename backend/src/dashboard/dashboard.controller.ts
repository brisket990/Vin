import { Controller, Get } from '@nestjs/common';
import { DashboardService } from './dashboard.service.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('dashboard')
export class DashboardController {
  constructor(private readonly dashboardService: DashboardService) {}

  @Get()
  getStats(@CurrentUser() user: AuthenticatedUser) {
    return this.dashboardService.getStats(user.householdId);
  }
}
