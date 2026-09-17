import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import { BottleService } from './bottle.service.js';
import { CreateBottleDto } from './dto/create-bottle.dto.js';
import { UpdateBottleDto } from './dto/update-bottle.dto.js';
import { QueryBottlesDto } from './dto/query-bottles.dto.js';
import { ConsumeBottleDto } from './dto/consume-bottle.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('bottles')
export class BottleController {
  constructor(private readonly bottleService: BottleService) {}

  @Post()
  create(@CurrentUser() user: AuthenticatedUser, @Body() dto: CreateBottleDto) {
    return this.bottleService.create(user.householdId, dto);
  }

  @Get()
  findAll(
    @CurrentUser() user: AuthenticatedUser,
    @Query() query: QueryBottlesDto,
  ) {
    return this.bottleService.findAll(user.householdId, query);
  }

  // Declared before ':id' -- a literal segment ("needing-turn") would
  // otherwise never be reached if Nest tried the dynamic route first.
  @Get('needing-turn')
  findNeedingTurn(@CurrentUser() user: AuthenticatedUser) {
    return this.bottleService.findNeedingTurn(user.householdId);
  }

  @Get(':id')
  findOne(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.bottleService.findOne(user.householdId, id);
  }

  @Patch(':id')
  update(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: UpdateBottleDto,
  ) {
    return this.bottleService.update(user.householdId, id, dto);
  }

  @Delete(':id')
  remove(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.bottleService.remove(user.householdId, id);
  }

  @Post(':id/consume')
  consume(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: ConsumeBottleDto,
  ) {
    return this.bottleService.consume(user.householdId, id, user.id, dto);
  }

  /** "Quart de tour" reminder: marks the bottle as turned today. */
  @Post(':id/turn')
  turn(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.bottleService.turn(user.householdId, id);
  }
}
