import {
  BadRequestException,
  Controller,
  Post,
  UploadedFile,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { memoryStorage } from 'multer';
import { ImportService } from './import.service.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

const MAX_CSV_SIZE_BYTES = 5 * 1024 * 1024;

@Controller('import')
export class ImportController {
  constructor(private readonly importService: ImportService) {}

  /** Imports bottles from a CSV file in the same column format the export
   *  endpoints produce (GET export/cellar.csv) -- see ImportService for the
   *  exact rules. Returns a summary rather than throwing on a bad row, so a
   *  handful of typos don't block the rest of the file. */
  @Post('cellar/csv')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: memoryStorage(),
      limits: { fileSize: MAX_CSV_SIZE_BYTES },
    }),
  )
  async importCsv(
    @CurrentUser() user: AuthenticatedUser,
    @UploadedFile() file: Express.Multer.File,
  ) {
    if (!file) {
      throw new BadRequestException('Aucun fichier reçu.');
    }
    const csvText = file.buffer.toString('utf-8');
    return this.importService.importCsv(user.householdId, csvText);
  }
}
