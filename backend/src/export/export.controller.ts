import { Controller, Get, Res } from '@nestjs/common';
import type { Response } from 'express';
import { ExportService } from './export.service.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('export')
export class ExportController {
  constructor(private readonly exportService: ExportService) {}

  @Get('cellar.csv')
  async exportCsv(@CurrentUser() user: AuthenticatedUser, @Res() res: Response) {
    const csv = await this.exportService.exportCsv(user.householdId);
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename="cave.csv"');
    res.send(csv);
  }

  @Get('cellar.pdf')
  async exportPdf(@CurrentUser() user: AuthenticatedUser, @Res() res: Response) {
    const pdf = await this.exportService.exportPdf(user.householdId);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', 'attachment; filename="cave.pdf"');
    res.send(pdf);
  }
}
