import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AnthropicProviderClient } from './anthropic.client.js';

describe('AnthropicProviderClient', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  it('sends the image and prompt, and parses the structured JSON response', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        content: [
          {
            type: 'text',
            text: '{"name":"Château Test","color":"red","vintage":2018,"confidence":"high"}',
          },
        ],
      }),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    const client = new AnthropicProviderClient('fake-key', 'claude-test-model');
    const result = await client.recognizeLabel('base64data', 'image/jpeg');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('https://api.anthropic.com/v1/messages');
    expect(options.headers['x-api-key']).toBe('fake-key');

    const body = JSON.parse(options.body);
    expect(body.model).toBe('claude-test-model');
    expect(body.messages[0].content[0].source.data).toBe('base64data');

    expect(result.structured.name).toBe('Château Test');
    expect(result.structured.color).toBe('red');
    expect(result.structured.vintage).toBe(2018);
  });

  it('sends the recipe prompt and parses the structured recipe suggestion', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        content: [
          {
            type: 'text',
            text: '{"recipeTitle":"Canard rôti aux cerises","recipeDescription":"Rôtir un canard...","reasoning":"S\'accorde avec les tanins."}',
          },
        ],
      }),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    const client = new AnthropicProviderClient('fake-key', 'claude-test-model');
    const result = await client.suggestRecipeForBottle({
      id: 'b1',
      name: 'Château Test',
      producer: null,
      region: null,
      color: 'red',
      grapeVarieties: null,
      vintage: 2018,
      quantity: 1,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(result.structured.recipeTitle).toBe('Canard rôti aux cerises');
    expect(result.structured.reasoning).toContain('tanins');
  });

  it('throws an UnauthorizedException-like error when the API key is rejected', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({}),
      text: async () => 'invalid x-api-key',
    }) as unknown as typeof fetch;

    const client = new AnthropicProviderClient('bad-key', 'claude-test-model');
    await expect(client.recognizeLabel('base64data', 'image/jpeg')).rejects.toThrow();
  });
});
