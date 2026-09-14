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

interface GeminiResponse {
  candidates: { content: { parts: { text?: string }[] } }[];
}

export class GoogleProviderClient implements AIProviderClient {
  constructor(
    private readonly apiKey: string,
    private readonly model: string,
  ) {}

  private url(): string {
    return `https://generativelanguage.googleapis.com/v1beta/models/${this.model}:generateContent?key=${this.apiKey}`;
  }

  private extractText(response: GeminiResponse): string {
    const text = response.candidates?.[0]?.content?.parts
      ?.map((part) => part.text ?? '')
      .join('');
    if (!text) throw new Error('Réponse Gemini vide ou inattendue.');
    return text;
  }

  async recognizeLabel(
    imageBase64: string,
    mimeType: string,
  ): Promise<RecognitionResult> {
    const response = (await postJson(this.url(), {}, {
      contents: [
        {
          parts: [
            { text: RECOGNITION_SYSTEM_PROMPT },
            { inlineData: { mimeType, data: imageBase64 } },
          ],
        },
      ],
    })) as GeminiResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<RecognizedWineFields>(rawResponse), rawResponse };
  }

  async suggestPairing(
    dish: string,
    candidates: PairingCandidateBottle[],
  ): Promise<PairingResult> {
    const response = (await postJson(this.url(), {}, {
      contents: [{ parts: [{ text: buildPairingPrompt(dish, candidates) }] }],
    })) as GeminiResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<PairingStructured>(rawResponse), rawResponse };
  }

  async suggestFoodForBottle(bottle: PairingCandidateBottle): Promise<FoodPairingResult> {
    const response = (await postJson(this.url(), {}, {
      contents: [{ parts: [{ text: buildFoodPairingPrompt(bottle) }] }],
    })) as GeminiResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<FoodPairingStructured>(rawResponse), rawResponse };
  }
}
