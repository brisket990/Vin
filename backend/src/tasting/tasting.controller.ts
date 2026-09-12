import { Body, Controller, Delete, Get, Param, Patch } from '@nestjs/common';
import { TastingService } from './tasting.service.js';
import { UpdateTastingNoteDto } from './dto/update-tasting-note.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('tasting-notes')
export class TastingController {
  constructor(private readonly tastingService: TastingService) {}

  @Get()
  findAll(@CurrentUser() user: AuthenticatedUser) {
    return this.tastingService.findAll(user.householdId);
  }

  @Patch(':id')
  update(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: UpdateTastingNoteDto,
  ) {
    return this.tastingService.update(user.householdId, id, dto);
  }

  @Delete(':id')
  remove(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.tastingService.remove(user.householdId, id);
  }
}
