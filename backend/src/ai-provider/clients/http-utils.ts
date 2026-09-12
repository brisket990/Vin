import { BadGatewayException, UnauthorizedException } from '@nestjs/common';

/**
 * Thin wrapper around fetch for calling a provider's REST API. Surfaces
 * authentication failures distinctly (so the app can tell the user their key
 * is wrong) and wraps everything else in a BadGatewayException with the
 * provider's own error body for debuggability.
 */
export async function postJson(
  url: string,
  headers: Record<string, string>,
  body: unknown,
): Promise<unknown> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...headers },
    body: JSON.stringify(body),
  });

  if (response.status === 401 || response.status === 403) {
    throw new UnauthorizedException(
      "Clé API refusée par le fournisseur IA -- vérifie la clé configurée dans les réglages.",
    );
  }

  if (!response.ok) {
    const text = await response.text();
    throw new BadGatewayException(
      `Le fournisseur IA a répondu une erreur (${response.status}) : ${text.slice(0, 500)}`,
    );
  }

  return response.json();
}
