import { OpenAiCompatibleClient } from './openai-compatible.client.js';

const API_URL = 'https://api.mistral.ai/v1/chat/completions';

/** French provider -- Pixtral models support image input for label scanning. */
export class MistralProviderClient extends OpenAiCompatibleClient {
  constructor(apiKey: string, model: string) {
    super(apiKey, model, API_URL, 'Mistral', true);
  }
}
