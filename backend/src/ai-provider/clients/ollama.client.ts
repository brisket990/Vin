import { BadRequestException } from '@nestjs/common';
import { OpenAiCompatibleClient } from './openai-compatible.client.js';

/**
 * A household's own self-hosted Ollama server (e.g. on the same box as
 * Coolify). No API key needed on a local network -- the household instead
 * configures the server's base URL in Réglages > Fournisseurs IA. Whether
 * the scan works depends on which model they've pulled locally (llava,
 * qwen2.5vl, llama3.2-vision, ... support images; most text models don't) --
 * Ollama's own error message on an unsupported model surfaces as-is.
 */
export class OllamaProviderClient extends OpenAiCompatibleClient {
  constructor(apiKey: string, model: string, baseUrl: string | null | undefined) {
    if (!baseUrl) {
      throw new BadRequestException(
        "Adresse du serveur Ollama manquante -- configure-la dans Réglages > Fournisseurs IA.",
      );
    }
    super(apiKey, model, `${baseUrl.replace(/\/+$/, '')}/v1/chat/completions`, 'Ollama', true);
  }
}
