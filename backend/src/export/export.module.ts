import { Module } from '@nestjs/common';
import { ExportService } from './export.service.js';
import { ExportController } from './export.controller.js';

@Module({
  providers: [ExportService],
  controllers: [ExportController],
})
export class ExportModule {}
