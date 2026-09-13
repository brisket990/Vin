import type { aiProviderValues } from '../db/schema.js';

/**
 * Default model per provider, used when the household hasn't overridden it
 * in Réglages > Fournisseurs IA. Model names/availability shift often --
 * these are reasonable defaults as of writing, but keep the `model` field on
 * AIProviderConfig editable so a stale default never hard-blocks the app.
 */
export const DEFAULT_MODELS: Record<(typeof aiProviderValues)[number], string> = {
  anthropic: 'claude-sonnet-4-5',
  openai: 'gpt-4o',
  // gemini-2.0-flash was shut down by Google on 2026-06-01 (404 on
  // generateContent) -- gemini-3.8-flash is the current GA default that
  // still supports image input for label recognition.
  google: 'gemini-3.8-flash',
  // "-latest" aliases auto-track Mistral's newest release under that name.
  mistral: 'pixtral-large-latest',
  // OpenRouter model IDs are "<vendor>/<model>" -- this picks a broadly
  // available, vision-capable one; the household can point it at any model
  // OpenRouter offers via the same field.
  openrouter: 'openai/gpt-4o-mini',
  deepseek: 'deepseek-flash',
  // Most people's default local pull; vision needs a model that actually
  // supports images (llava, qwen2.5vl, llama3.2-vision, ...) set explicitly.
  ollama: 'llama3.2',
};
