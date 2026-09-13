import { OpenAiCompatibleClient } from './openai-compatible.client.js';

const API_URL = 'https://api.deepseek.com/chat/completions';

/** deepseek-flash supports image input, so scan + accords both work. */
export class DeepSeekProviderClient extends OpenAiCompatibleClient {
  constructor(apiKey: string, model: string) {
    super(apiKey, model, API_URL, 'DeepSeek', true);
  }
}
