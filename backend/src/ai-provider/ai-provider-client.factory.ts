import { AnthropicProviderClient } from './clients/anthropic.client.js';
import { OpenAiProviderClient } from './clients/openai.client.js';
import { GoogleProviderClient } from './clients/google.client.js';
import { MistralProviderClient } from './clients/mistral.client.js';
import { OpenRouterProviderClient } from './clients/openrouter.client.js';
import { DeepSeekProviderClient } from './clients/deepseek.client.js';
import { OllamaProviderClient } from './clients/ollama.client.js';
import type { AIProviderClient } from './types.js';
import type { aiProviderValues } from '../db/schema.js';

export function createProviderClient(
  provider: (typeof aiProviderValues)[number],
  apiKey: string,
  model: string,
  baseUrl?: string | null,
): AIProviderClient {
  switch (provider) {
    case 'anthropic':
      return new AnthropicProviderClient(apiKey, model);
    case 'openai':
      return new OpenAiProviderClient(apiKey, model);
    case 'google':
      return new GoogleProviderClient(apiKey, model);
    case 'mistral':
      return new MistralProviderClient(apiKey, model);
    case 'openrouter':
      return new OpenRouterProviderClient(apiKey, model);
    case 'deepseek':
      return new DeepSeekProviderClient(apiKey, model);
    case 'ollama':
      return new OllamaProviderClient(apiKey, model, baseUrl);
  }
}
