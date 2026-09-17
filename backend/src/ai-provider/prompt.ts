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
  "drinkFromYear": number | null,
  "drinkUntilYear": number | null,
  "foodPairings": string[] | null,
  "tastingNose": string | null,
  "tastingPalate": string | null,
  "tastingSweetness": string | null,
  "confidence": "low" | "medium" | "high",
  "notes": string | null
}

Pour "drinkFromYear" et "drinkUntilYear" : si le millésime est identifiable ou déductible, estime la fenêtre d'apogée (en années calendaires pleines, ex. 2026 et 2032) à partir du style du vin, de la région, du cépage et du millésime -- c'est une estimation de sommelier, mets null si vraiment impossible à évaluer (ex. millésime inconnu).
Pour "foodPairings" : propose 2 à 4 accords mets-vin courts et concrets (ex. "fromages affinés", "poisson grillé", "viande rouge en sauce"), du plus au moins évident.
Pour "tastingNose" : décris en une phrase les arômes attendus au nez (fruits, fleurs, épices, bois, etc.), typiques de ce style de vin/cépage/région/millésime.
Pour "tastingPalate" : décris en une phrase la bouche attendue (structure, tanins, acidité, corps, longueur).
Pour "tastingSweetness" : un mot ou une courte expression sur le niveau de sucrosité (ex. "sec", "demi-sec", "moelleux", "liquoreux") adapté à la couleur/au style du vin.
Ces trois champs sont une estimation de sommelier basée sur le style du vin, pas une dégustation réelle -- mets null uniquement si le style est vraiment indéterminable.
Pour "notes" : rédige 2 à 3 phrases en français décrivant l'origine, le terroir et le style du vin (caractère, corps, potentiel de garde) pour enrichir sa fiche -- pas juste un résumé des autres champs.

Attention aux vins jaunes du Jura (appellations "Château-Chalon", "Côtes du Jura" mention "Vin Jaune", "Arbois" mention "Vin Jaune", "L'Étoile" mention "Vin Jaune") : réglementairement ce sont des vins blancs, donc "color" reste bien "white" -- MAIS ne les traite jamais comme un vin blanc générique. Repère les indices sur l'étiquette (mention "Vin Jaune", "sous voile", clavelin 62cl, "Château-Chalon", etc.) et si tu identifies un vin jaune :
- mets impérativement "Vin Jaune" bien visible dans "appellation" (ex. "Château-Chalon" ou "Côtes du Jura - Vin Jaune"), jamais juste la région/appellation de base sans le préciser ;
- dans "notes", nomme explicitement "vin jaune" et son élevage caractéristique sous voile de levures (au moins 6 ans et 3 mois en fût sans ouillage) -- ne le décris jamais comme un simple vin blanc sec ;
- pour "tastingNose"/"tastingPalate", utilise les descripteurs typiques du vin jaune (noix, curry, épices, garrigue, le fameux "goût de jaune") plutôt que des descripteurs de vin blanc classique ;
- pour "tastingSweetness", "sec" reste correct mais peut être précisé (ex. "sec, oxydatif").`;

export function buildPairingPrompt(
  dish: string,
  candidates: PairingCandidateBottle[],
): string {
  return `Tu es un sommelier expert. La personne va manger : "${dish}".

Voici, au format JSON, les bouteilles actuellement disponibles dans sa cave :
${JSON.stringify(candidates, null, 2)}

Réponds en deux parties, UNIQUEMENT avec un objet JSON valide (rien d'autre, pas de texte ni de balises markdown autour), au format exact suivant :
{
  "cellarSuggestions": [
    { "bottleId": string, "score": number, "reasoning": string }
  ],
  "shoppingSuggestions": [
    {
      "name": string,
      "color": "red" | "white" | "rose" | "sparkling" | "sweet" | "fortified",
      "region": string | null,
      "grapeVarieties": string[] | null,
      "score": number,
      "reasoning": string
    }
  ]
}

Pour "cellarSuggestions" : choisis jusqu'à 3 bouteilles PARMI CELLES LISTÉES CI-DESSUS UNIQUEMENT (aucun id inventé), classées de la meilleure à la moins bonne. S'il y a moins de 3 bouteilles vraiment pertinentes, n'en propose que le nombre pertinent (peut être 0 si rien ne convient).
Pour "shoppingSuggestions" : indépendamment de la cave, propose jusqu'à 3 vins que la personne pourrait acheter en magasin pour ce plat -- décris chacun par son style/cépage/région typiques plutôt qu'une marque précise (ex. "Un Chablis, Chardonnay de Bourgogne"), classés de la meilleure à la moins bonne suggestion.
Pour "score" (dans les deux listes) : une note de 1 à 10 reflétant la qualité de l'accord. Étale bien les notes pour montrer l'écart entre la meilleure suggestion et les suivantes (par exemple 9, 7, 6 plutôt que 8, 8, 8) -- ne mets jamais la même note à toutes les suggestions d'une même liste.
"reasoning" est une explication concise en français, spécifique à cette bouteille ou ce vin précis (pas une explication générique répétée d'une suggestion à l'autre).`;
}

export function buildFoodPairingPrompt(bottle: PairingCandidateBottle): string {
  return `Tu es un sommelier expert. Voici une bouteille de vin, au format JSON :
${JSON.stringify(bottle, null, 2)}

Propose les meilleurs accords mets-vin pour cette bouteille précise. Réponds UNIQUEMENT avec un objet JSON valide (rien d'autre), au format exact suivant :
{
  "suggestedDishes": string[],
  "reasoning": string
}
"suggestedDishes" est une liste de 2 à 5 plats ou types de plats qui se marient bien avec ce vin, du plus au moins recommandé. "reasoning" est une explication concise en français de ces choix, tenant compte du cépage, de la région, de la couleur et du millésime.`;
}

/**
 * Used for "apogée" (drinking window) alerts: unlike buildFoodPairingPrompt
 * (short dish names), this asks for one fully-formed recipe idea worth
 * putting effort into, since it's sent at most twice in the bottle's
 * lifetime rather than on every lookup.
 */
export function buildRecipePrompt(bottle: PairingCandidateBottle): string {
  return `Tu es un chef cuisinier et sommelier expert. Voici une bouteille de vin, au format JSON :
${JSON.stringify(bottle, null, 2)}

Ce vin arrive dans (ou termine bientôt) sa fenêtre de dégustation optimale. Propose UNE idée de recette élaborée qui se marie particulièrement bien avec cette bouteille précise -- pas juste un nom de plat, une vraie idée de recette. Réponds UNIQUEMENT avec un objet JSON valide (rien d'autre), au format exact suivant :
{
  "recipeTitle": string,
  "recipeDescription": string,
  "reasoning": string
}
"recipeTitle" est le nom du plat (court, appétissant). "recipeDescription" décrit la recette en 3 à 5 phrases : ingrédients principaux et grandes étapes de préparation, suffisant pour donner une vraie idée réalisable sans être un livre de cuisine complet. "reasoning" est une explication concise en français de l'accord, tenant compte du cépage, de la région, de la couleur et du millésime.`;
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
