import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  UploadedFile,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { memoryStorage } from 'multer';
import { ScanService } from './scan.service.js';
import { ScanPhotoDto } from './dto/scan-photo.dto.js';
import { LinkScanBottleDto } from './dto/link-scan-bottle.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

const MAX_PHOTO_SIZE_BYTES = 10 * 1024 * 1024;

@Controller('scan')
export class ScanController {
  constructor(private readonly scanService: ScanService) {}

  @Post()
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: memoryStorage(),
      limits: { fileSize: MAX_PHOTO_SIZE_BYTES },
    }),
  )
  scan(
    @CurrentUser() user: AuthenticatedUser,
    @UploadedFile() file: Express.Multer.File,
    @Body() dto: ScanPhotoDto,
  ) {
    return this.scanService.scan(user.householdId, file, dto);
  }

  @Get()
  findAll(@CurrentUser() user: AuthenticatedUser) {
    return this.scanService.findAll(user.householdId);
  }

  @Get(':id')
  findOne(@CurrentUser() user: AuthenticatedUser, @Param('id') id: string) {
    return this.scanService.findOne(user.householdId, id);
  }

  @Patch(':id/link-bottle')
  linkBottle(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') id: string,
    @Body() dto: LinkScanBottleDto,
  ) {
    return this.scanService.linkBottle(user.householdId, id, dto.bottleId);
  }
}
