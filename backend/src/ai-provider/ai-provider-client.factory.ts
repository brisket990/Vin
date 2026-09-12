import { AnthropicProviderClient } from './clients/anthropic.client.js';
import { OpenAiProviderClient } from './clients/openai.client.js';
import { GoogleProviderClient } from './clients/google.client.js';
import type { AIProviderClient } from './types.js';
import type { aiProviderValues } from '../db/schema.js';

export function createProviderClient(
  provider: (typeof aiProviderValues)[number],
  apiKey: string,
  model: string,
): AIProviderClient {
  switch (provider) {
    case 'anthropic':
      return new AnthropicProviderClient(apiKey, model);
    case 'openai':
      return new OpenAiProviderClient(apiKey, model);
    case 'google':
      return new GoogleProviderClient(apiKey, model);
  }
}
