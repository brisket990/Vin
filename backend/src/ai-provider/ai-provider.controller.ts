import { Body, Controller, Delete, Get, Param, Post } from '@nestjs/common';
import { AiProviderConfigService } from './ai-provider-config.service.js';
import { UpsertAiProviderConfigDto } from './dto/upsert-ai-provider-config.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('ai-providers')
export class AiProviderController {
  constructor(private readonly configService: AiProviderConfigService) {}

  @Get()
  list(@CurrentUser() user: AuthenticatedUser) {
    return this.configService.list(user.householdId);
  }

  @Post()
  upsert(
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: UpsertAiProviderConfigDto,
  ) {
    return this.configService.upsert(user.householdId, dto);
  }

  @Delete(':id')
  remove(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.configService.remove(user.householdId, id);
  }
}
