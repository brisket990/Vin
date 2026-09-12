import { Body, Controller, Get, Patch, Post } from '@nestjs/common';
import { HouseholdService } from './household.service.js';
import { UpdateHouseholdDto } from './dto/update-household.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('household')
export class HouseholdController {
  constructor(private readonly householdService: HouseholdService) {}

  @Get('me')
  getMine(@CurrentUser() user: AuthenticatedUser) {
    return this.householdService.getHousehold(user.householdId);
  }

  @Roles('owner')
  @Patch('me')
  rename(@CurrentUser() user: AuthenticatedUser, @Body() dto: UpdateHouseholdDto) {
    return this.householdService.rename(user.householdId, dto.name);
  }

  @Roles('owner')
  @Post('me/regenerate-invite-code')
  regenerateInviteCode(@CurrentUser() user: AuthenticatedUser) {
    return this.householdService.regenerateInviteCode(user.householdId);
  }
}
