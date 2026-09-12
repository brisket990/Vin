import { Body, Controller, Get, Param, Post } from '@nestjs/common';
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

  @Get()
  findAll(@CurrentUser() user: AuthenticatedUser) {
    return this.pairingService.findAll(user.householdId);
  }

  @Get(':id')
  findOne(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.pairingService.findOne(user.householdId, id);
  }
}
