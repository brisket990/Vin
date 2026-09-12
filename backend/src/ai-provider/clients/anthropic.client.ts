import {
  RECOGNITION_SYSTEM_PROMPT,
  buildPairingPrompt,
  extractJson,
} from '../prompt.js';
import type {
  AIProviderClient,
  PairingCandidateBottle,
  PairingResult,
  PairingStructured,
  RecognitionResult,
  RecognizedWineFields,
} from '../types.js';
import { postJson } from './http-utils.js';

const API_URL = 'https://api.anthropic.com/v1/messages';
const ANTHROPIC_VERSION = '2023-06-01';

interface AnthropicMessageResponse {
  content: { type: string; text?: string }[];
}

export class AnthropicProviderClient implements AIProviderClient {
  constructor(
    private readonly apiKey: string,
    private readonly model: string,
  ) {}

  private headers() {
    return {
      'x-api-key': this.apiKey,
      'anthropic-version': ANTHROPIC_VERSION,
    };
  }

  private extractText(response: AnthropicMessageResponse): string {
    const textBlock = response.content.find((block) => block.type === 'text');
    if (!textBlock?.text) {
      throw new Error("Réponse Anthropic vide ou inattendue.");
    }
    return textBlock.text;
  }

  async recognizeLabel(
    imageBase64: string,
    mimeType: string,
  ): Promise<RecognitionResult> {
    const response = (await postJson(API_URL, this.headers(), {
      model: this.model,
      max_tokens: 1024,
      messages: [
        {
          role: 'user',
          content: [
            {
              type: 'image',
              source: { type: 'base64', media_type: mimeType, data: imageBase64 },
            },
            { type: 'text', text: RECOGNITION_SYSTEM_PROMPT },
          ],
        },
      ],
    })) as AnthropicMessageResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<RecognizedWineFields>(rawResponse), rawResponse };
  }

  async suggestPairing(
    dish: string,
    candidates: PairingCandidateBottle[],
  ): Promise<PairingResult> {
    const response = (await postJson(API_URL, this.headers(), {
      model: this.model,
      max_tokens: 1024,
      messages: [
        { role: 'user', content: buildPairingPrompt(dish, candidates) },
      ],
    })) as AnthropicMessageResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<PairingStructured>(rawResponse), rawResponse };
  }
}
