import { BadRequestException } from '@nestjs/common';
import {
  RECOGNITION_SYSTEM_PROMPT,
  buildFoodPairingPrompt,
  buildPairingPrompt,
  buildRecipePrompt,
  extractJson,
} from '../prompt.js';
import type {
  AIProviderClient,
  FoodPairingResult,
  FoodPairingStructured,
  PairingCandidateBottle,
  PairingResult,
  PairingStructured,
  RecipeSuggestionResult,
  RecipeSuggestionStructured,
  RecognitionResult,
  RecognizedWineFields,
} from '../types.js';
import { postJson } from './http-utils.js';

interface ChatCompletionsResponse {
  choices: { message: { content: string } }[];
}

/**
 * Base for any provider exposing an OpenAI-compatible /chat/completions
 * endpoint (Mistral, OpenRouter, DeepSeek, and Ollama's own OpenAI-compat
 * API all do). Subclasses just supply the URL, headers and vision support --
 * the request/response shape is shared.
 */
export class OpenAiCompatibleClient implements AIProviderClient {
  constructor(
    protected readonly apiKey: string,
    protected readonly model: string,
    private readonly chatUrl: string,
    private readonly providerLabel: string,
    private readonly supportsVision: boolean = true,
    private readonly extraHeaders: Record<string, string> = {},
  ) {}

  protected headers(): Record<string, string> {
    return {
      ...(this.apiKey ? { authorization: `Bearer ${this.apiKey}` } : {}),
      ...this.extraHeaders,
    };
  }

  private extractText(response: ChatCompletionsResponse): string {
    const content = response.choices?.[0]?.message?.content;
    if (!content) {
      throw new Error(`Réponse ${this.providerLabel} vide ou inattendue.`);
    }
    return content;
  }

  async recognizeLabel(
    imageBase64: string,
    mimeType: string,
  ): Promise<RecognitionResult> {
    if (!this.supportsVision) {
      throw new BadRequestException(
        `${this.providerLabel} ne prend pas en charge la reconnaissance d'image -- choisis un autre fournisseur (ou un autre modèle) pour le scan d'étiquette.`,
      );
    }

    const response = (await postJson(this.chatUrl, this.headers(), {
      model: this.model,
      max_tokens: 2048,
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
    })) as ChatCompletionsResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<RecognizedWineFields>(rawResponse), rawResponse };
  }

  async suggestPairing(
    dish: string,
    candidates: PairingCandidateBottle[],
  ): Promise<PairingResult> {
    const response = (await postJson(this.chatUrl, this.headers(), {
      model: this.model,
      max_tokens: 2048,
      messages: [{ role: 'user', content: buildPairingPrompt(dish, candidates) }],
    })) as ChatCompletionsResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<PairingStructured>(rawResponse), rawResponse };
  }

  async suggestFoodForBottle(bottle: PairingCandidateBottle): Promise<FoodPairingResult> {
    const response = (await postJson(this.chatUrl, this.headers(), {
      model: this.model,
      max_tokens: 1024,
      messages: [{ role: 'user', content: buildFoodPairingPrompt(bottle) }],
    })) as ChatCompletionsResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<FoodPairingStructured>(rawResponse), rawResponse };
  }

  async suggestRecipeForBottle(bottle: PairingCandidateBottle): Promise<RecipeSuggestionResult> {
    const response = (await postJson(this.chatUrl, this.headers(), {
      model: this.model,
      max_tokens: 1024,
      messages: [{ role: 'user', content: buildRecipePrompt(bottle) }],
    })) as ChatCompletionsResponse;

    const rawResponse = this.extractText(response);
    return { structured: extractJson<RecipeSuggestionStructured>(rawResponse), rawResponse };
  }
}
