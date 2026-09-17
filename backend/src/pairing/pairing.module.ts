import { Module } from '@nestjs/common';
import { PairingService } from './pairing.service.js';
import { PairingController } from './pairing.controller.js';
import { AiProviderModule } from '../ai-provider/ai-provider.module.js';

@Module({
  imports: [AiProviderModule],
  providers: [PairingService],
  controllers: [PairingController],
  exports: [PairingService],
})
export class PairingModule {}
