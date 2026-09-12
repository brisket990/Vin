import { Module } from '@nestjs/common';
import { AiProviderConfigService } from './ai-provider-config.service.js';
import { AiProviderService } from './ai-provider.service.js';
import { AiProviderController } from './ai-provider.controller.js';

@Module({
  providers: [AiProviderConfigService, AiProviderService],
  controllers: [AiProviderController],
  exports: [AiProviderConfigService, AiProviderService],
})
export class AiProviderModule {}
