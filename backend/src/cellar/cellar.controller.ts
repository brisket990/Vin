import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { CellarService } from './cellar.service.js';
import { CreateCellarUnitDto } from './dto/create-cellar-unit.dto.js';
import { SuggestLocationDto } from './dto/suggest-location.dto.js';
import { NextFreeLocationsDto } from './dto/next-free-locations.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('cellar/units')
export class CellarController {
  constructor(private readonly cellarService: CellarService) {}

  @Get()
  list(@CurrentUser() user: AuthenticatedUser) {
    return this.cellarService.listUnits(user.householdId);
  }

  @Post()
  create(@CurrentUser() user: AuthenticatedUser, @Body() dto: CreateCellarUnitDto) {
    return this.cellarService.createUnit(user.householdId, dto);
  }

  @Get(':id')
  get(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.cellarService.getUnit(user.householdId, id);
  }

  @Post(':id/suggest-location')
  suggest(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: SuggestLocationDto,
  ) {
    return this.cellarService.suggestLocations(user.householdId, id, dto);
  }
}

/**
 * Separate controller (different path prefix: cellar/locations rather than
 * cellar/units) for operations that address a single location directly
 * rather than a unit.
 */
@Controller('cellar/locations')
export class CellarLocationsController {
  constructor(private readonly cellarService: CellarService) {}

  @Post(':id/next-free')
  nextFree(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: NextFreeLocationsDto,
  ) {
    return this.cellarService.findNextFreeLocations(user.householdId, id, dto.count);
  }
}
