import { Module } from '@nestjs/common';
import { ScanService } from './scan.service.js';
import { ScanController } from './scan.controller.js';
import { StorageModule } from '../storage/storage.module.js';
import { AiProviderModule } from '../ai-provider/ai-provider.module.js';

@Module({
  imports: [StorageModule, AiProviderModule],
  providers: [ScanService],
  controllers: [ScanController],
})
export class ScanModule {}
