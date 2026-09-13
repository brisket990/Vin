import { afterEach, describe, expect, it, vi } from 'vitest';
import { OpenAiCompatibleClient } from './openai-compatible.client.js';
import { DeepSeekProviderClient } from './deepseek.client.js';
import { OllamaProviderClient } from './ollama.client.js';

describe('OpenAiCompatibleClient', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  it('sends the image as an image_url data URI and parses the structured JSON response', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        choices: [
          {
            message: {
              content: '{"name":"Château Test","color":"red","vintage":2018,"confidence":"high"}',
            },
          },
        ],
      }),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    const client = new DeepSeekProviderClient('fake-key', 'deepseek-flash');
    const result = await client.recognizeLabel('base64data', 'image/jpeg');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('https://api.deepseek.com/chat/completions');
    expect(options.headers.authorization).toBe('Bearer fake-key');

    const body = JSON.parse(options.body);
    expect(body.model).toBe('deepseek-flash');
    expect(body.messages[1].content[0].image_url.url).toBe('data:image/jpeg;base64,base64data');

    expect(result.structured.name).toBe('Château Test');
    expect(result.structured.color).toBe('red');
  });

  it('rejects recognizeLabel with a clear message when the provider has no vision support', async () => {
    const client = new OpenAiCompatibleClient(
      'fake-key',
      'text-only-model',
      'https://example.invalid/chat/completions',
      'TestProvider',
      false,
    );

    await expect(client.recognizeLabel('base64data', 'image/jpeg')).rejects.toThrow(
      /TestProvider ne prend pas en charge/,
    );
  });

  it('Ollama refuses to build a client without a configured base URL', () => {
    expect(() => new OllamaProviderClient('', 'llama3.2', undefined)).toThrow(
      /Adresse du serveur Ollama manquante/,
    );
  });

  it('Ollama targets the OpenAI-compatible endpoint under the configured base URL, no auth header when no key', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ choices: [{ message: { content: '{"suggestedBottleIds":[],"reasoning":"ok"}' } }] }),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    const client = new OllamaProviderClient('', 'llama3.2', 'http://192.168.1.50:11434/');
    await client.suggestPairing('magret de canard', []);

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('http://192.168.1.50:11434/v1/chat/completions');
    expect(options.headers.authorization).toBeUndefined();
  });
});
