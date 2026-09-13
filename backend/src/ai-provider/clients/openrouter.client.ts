import { OpenAiCompatibleClient } from './openai-compatible.client.js';

const API_URL = 'https://openrouter.ai/api/v1/chat/completions';

/**
 * OpenRouter proxies dozens of underlying models (Claude, GPT, Gemini,
 * Llama, ...) behind one API key -- the `model` field picks which one,
 * e.g. "openai/gpt-4o-mini" or "anthropic/claude-3.5-haiku". Vision support
 * then depends entirely on the chosen underlying model.
 */
export class OpenRouterProviderClient extends OpenAiCompatibleClient {
  constructor(apiKey: string, model: string) {
    super(apiKey, model, API_URL, 'OpenRouter', true, {
      // Recommended (not required) by OpenRouter for attribution/rankings.
      'HTTP-Referer': 'https://github.com/brisket990/Vin',
      'X-Title': 'Vin - cave a vin',
    });
  }
}
