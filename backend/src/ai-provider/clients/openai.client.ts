import {
  RECOGNITION_SYSTEM_PROMPT,
  buildFoodPairingPrompt,
  buildPairingPrompt,
  extractJson,
} from '../prompt.js';
import type {
  AIProviderClient,
  FoodPairingResult,
  FoodPairingStructured,
  PairingCandidateBottle,
  PairingResult,
  PairingStructured,
  RecognitionResult,
  RecognizedWineFields,
} from '../types.js';
import { postJson } from './http-utils.js';

const API_URL = 'https://api.openai.com/v1/chat/completions';

interface OpenAiChatResponse {
  choices: { message: { content: string } }[];
}

export class OpenAiProviderClient implements AIProviderClient {
  constructor(
    private readonly apiKey: string,
    private readonly model: string,
  ) {}

  private headers() {
    return { authorization: `Bearer ${this.apiKey}` };
  }

  private extractText(response: OpenAiChatResponse): string {
    const content = response.choices?.[0]?.message?.content;
    if (!content) throw new Error('Réponse OpenAI vide ou inattendue.');
    return content;
  }

  async recognizeLabel(
    imageBase64: string,
    mimeType: string,
  ): Promise<RecognitionResult> {
    const response = (await postJson(API_URL, this.headers(), {
      model: this.model,
      max_tokens: 1024,
      messages: [
        { role: 'system', content: RECOGNITION_SYSTEM_PROMPT },
        {
          role: 'user',
          content: [
            {
              type: 'image_url',
              image_url: { url: `data:${mimeType};base64,${imageBase64}` },
            },
          ],
        },
      ],
    })) as OpenAiChatResponse;

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
      messages: [{ role: 'user', content: buildPairingPrompt(dish, candidates) }],
    })) as OpenAiChatResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<PairingStructured>(rawResponse), rawResponse };
  }

  async suggestFoodForBottle(bottle: PairingCandidateBottle): Promise<FoodPairingResult> {
    const response = (await postJson(API_URL, this.headers(), {
      model: this.model,
      max_tokens: 1024,
      messages: [{ role: 'user', content: buildFoodPairingPrompt(bottle) }],
    })) as OpenAiChatResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<FoodPairingStructured>(rawResponse), rawResponse };
  }
}
