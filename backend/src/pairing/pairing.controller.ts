import { Body, Controller, Get, Param, Post, Query } from '@nestjs/common';
import { PairingService } from './pairing.service.js';
import { CreatePairingDto } from './dto/create-pairing.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('pairing')
export class PairingController {
  constructor(private readonly pairingService: PairingService) {}

  @Post()
  suggest(@CurrentUser() user: AuthenticatedUser, @Body() dto: CreatePairingDto) {
    return this.pairingService.suggest(user.householdId, dto);
  }

  /** Reverse direction: given a bottle already in the cellar, ask the AI what food pairs well with it. */
  @Post('for-bottle/:bottleId')
  suggestForBottle(
    @CurrentUser() user: AuthenticatedUser,
    @Param('bottleId') bottleId: string,
    @Query('provider') provider?: string,
  ) {
    return this.pairingService.suggestForBottle(user.householdId, bottleId, provider);
  }

  /** Like for-bottle but a full recipe idea rather than a short dish name -- also used by the apogée alert job. */
  @Post('recipe-for-bottle/:bottleId')
  suggestRecipeForBottle(
    @CurrentUser() user: AuthenticatedUser,
    @Param('bottleId') bottleId: string,
    @Query('provider') provider?: string,
  ) {
    return this.pairingService.suggestRecipeForBottle(user.householdId, bottleId, provider);
  }

  @Get()
  findAll(@CurrentUser() user: AuthenticatedUser) {
    return this.pairingService.findAll(user.householdId);
  }

  @Get(':id')
  findOne(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.pairingService.findOne(user.householdId, id);
  }
}
