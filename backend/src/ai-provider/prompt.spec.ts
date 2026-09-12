import { describe, expect, it } from 'vitest';
import { extractJson } from './prompt.js';

describe('extractJson', () => {
  it('parses a plain JSON response', () => {
    const result = extractJson<{ a: number }>('{"a": 1}');
    expect(result).toEqual({ a: 1 });
  });

  it('extracts JSON wrapped in a markdown code fence', () => {
    const text = 'Voici le résultat :\n```json\n{"a": 2}\n```\nVoilà.';
    const result = extractJson<{ a: number }>(text);
    expect(result).toEqual({ a: 2 });
  });

  it('extracts a bare JSON object surrounded by prose', () => {
    const text = 'Bien sûr ! {"a": 3} en espérant que ça aide.';
    const result = extractJson<{ a: number }>(text);
    expect(result).toEqual({ a: 3 });
  });

  it('throws a clear error when no JSON can be found', () => {
    expect(() => extractJson('pas de JSON ici du tout')).toThrow(
      /JSON introuvable/,
    );
  });
});
