import { Injectable } from '@nestjs/common';
import { AiProviderConfigService } from './ai-provider-config.service.js';
import { createProviderClient } from './ai-provider-client.factory.js';
import type { aiUsageValues } from '../db/schema.js';

@Injectable()
export class AiProviderService {
  constructor(private readonly configService: AiProviderConfigService) {}

  async getClientForUsage(
    householdId: string,
    usage: (typeof aiUsageValues)[number],
    preferredProvider?: string,
  ) {
    const resolved = await this.configService.resolveForUsage(
      householdId,
      usage,
      preferredProvider,
    );
    return {
      provider: resolved.provider,
      client: createProviderClient(
        resolved.provider,
        resolved.apiKey,
        resolved.model,
        resolved.baseUrl,
      ),
    };
  }
}
