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
  google: 'gemini-2.0-flash',
};
