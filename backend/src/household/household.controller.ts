import { Body, Controller, Get, Patch, Post } from '@nestjs/common';
import { HouseholdService } from './household.service.js';
import { AuthService } from '../auth/auth.service.js';
import { UpdateHouseholdDto } from './dto/update-household.dto.js';
import { CreateHouseholdDto } from './dto/create-household.dto.js';
import { SwitchHouseholdDto } from './dto/switch-household.dto.js';
import { JoinHouseholdByCodeDto } from './dto/join-household-by-code.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import { Roles } from '../common/decorators/roles.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('household')
export class HouseholdController {
  constructor(
    private readonly householdService: HouseholdService,
    private readonly authService: AuthService,
  ) {}

  /** Every foyer this account belongs to -- drives the switcher UI. */
  @Get('mine')
  listMine(@CurrentUser() user: AuthenticatedUser) {
    return this.householdService.listMine(user.id);
  }

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

  /** Creates an additional foyer (e.g. "Appartement") owned by this account
   *  and immediately returns a token scoped to it, so the app can switch
   *  straight in without a second round trip. */
  @Post()
  async create(@CurrentUser() user: AuthenticatedUser, @Body() dto: CreateHouseholdDto) {
    const household = await this.householdService.createAdditional(user.id, dto.name);
    const session = await this.authService.issueTokenForHousehold(user.id, household.id, 'owner');
    return { household, ...session };
  }

  /** Joins an existing foyer with the CURRENT account via its invite code
   *  (e.g. a partner's account joining the "Appartement" foyer someone else
   *  created) and returns a token scoped to it. */
  @Post('join')
  async join(@CurrentUser() user: AuthenticatedUser, @Body() dto: JoinHouseholdByCodeDto) {
    const household = await this.householdService.joinByCode(user.id, dto.inviteCode);
    const session = await this.authService.issueTokenForHousehold(user.id, household.id, 'member');
    return { household, ...session };
  }

  /** Switches which of the user's foyers is active by issuing a token
   *  scoped to it -- the app replaces its stored token and refreshes every
   *  household-scoped screen (cave, envies, accords, tableau de bord). */
  @Post('switch')
  async switch(@CurrentUser() user: AuthenticatedUser, @Body() dto: SwitchHouseholdDto) {
    const membership = await this.householdService.assertMembership(user.id, dto.householdId);
    return this.authService.issueTokenForHousehold(user.id, dto.householdId, membership.role);
  }
}
