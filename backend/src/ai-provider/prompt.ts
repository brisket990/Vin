import type { PairingCandidateBottle } from './types.js';

export const RECOGNITION_SYSTEM_PROMPT = `Tu es un sommelier expert. On te montre la photo de l'étiquette d'une bouteille de vin.
Identifie les informations visibles ou déductibles et réponds UNIQUEMENT avec un objet JSON valide (rien d'autre : pas de texte, pas de balises markdown autour), au format exact suivant (mets null pour les champs inconnus) :
{
  "name": string | null,
  "producer": string | null,
  "region": string | null,
  "appellation": string | null,
  "grapeVarieties": string[] | null,
  "vintage": number | null,
  "color": "red" | "white" | "rose" | "sparkling" | "sweet" | "fortified" | null,
  "confidence": "low" | "medium" | "high",
  "notes": string | null
}`;

export function buildPairingPrompt(
  dish: string,
  candidates: PairingCandidateBottle[],
): string {
  return `Tu es un sommelier expert. La personne va manger : "${dish}".

Voici, au format JSON, les bouteilles actuellement disponibles dans sa cave (n'en propose aucune autre que celles listées ici) :
${JSON.stringify(candidates, null, 2)}

Choisis la ou les meilleures bouteilles de cette liste pour accompagner ce plat. Réponds UNIQUEMENT avec un objet JSON valide (rien d'autre), au format exact suivant :
{
  "suggestedBottleIds": string[],
  "reasoning": string
}
"suggestedBottleIds" doit contenir uniquement des ids présents dans la liste ci-dessus, du plus au moins recommandé. "reasoning" est une explication concise en français de ce choix.`;
}

/**
 * Extracts a JSON object from a model's text response. Models are
 * instructed to return raw JSON, but some wrap it in prose or a markdown
 * fence regardless -- this falls back to grabbing the first {...} block.
 */
export function extractJson<T>(text: string): T {
  const trimmed = text.trim();
  try {
    return JSON.parse(trimmed) as T;
  } catch {
    const fenced = trimmed.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
    if (fenced) {
      try {
        return JSON.parse(fenced[1]) as T;
      } catch {
        // fall through to brace matching below
      }
    }
    const match = trimmed.match(/\{[\s\S]*\}/);
    if (match) {
      return JSON.parse(match[0]) as T;
    }
    throw new Error("Réponse de l'IA non exploitable : JSON introuvable.");
  }
}
