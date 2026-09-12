import { Controller, Get, NotFoundException, Param, Res } from '@nestjs/common';
import type { Response } from 'express';
import { existsSync } from 'node:fs';
import { StorageService } from './storage.service.js';

// Photos are served to any authenticated user (global JwtAuthGuard applies).
// Filenames are random UUIDs, so this relies on non-guessability rather than
// per-household ACLs -- acceptable for a v1 self-hosted, two-person app.
@Controller('photos')
export class PhotosController {
  constructor(private readonly storageService: StorageService) {}

  @Get(':filename')
  get(@Param('filename') filename: string, @Res() res: Response) {
    const path = this.storageService.resolvePath(filename);
    if (!existsSync(path)) {
      throw new NotFoundException('Photo introuvable.');
    }
    res.sendFile(path);
  }
}
