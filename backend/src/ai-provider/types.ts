export interface RecognizedWineFields {
  name?: string;
  producer?: string;
  region?: string;
  appellation?: string;
  grapeVarieties?: string[];
  vintage?: number;
  color?: 'red' | 'white' | 'rose' | 'sparkling' | 'sweet' | 'fortified';
  /** Estimated drinking window, in calendar years -- a sommelier's guess
   *  based on style/region/grape/vintage, not a guarantee. */
  drinkFromYear?: number;
  drinkUntilYear?: number;
  /** 2-4 short food pairing suggestions (e.g. "fromages affinés"),
   *  available immediately at recognition time without a separate call. */
  foodPairings?: string[];
  /** Sommelier-style tasting profile, kept as three separate short
   *  categories rather than one blob so the fiche can display them on
   *  their own lines (nez / bouche / sucrosité). */
  tastingNose?: string;
  tastingPalate?: string;
  tastingSweetness?: string;
  confidence?: 'low' | 'medium' | 'high';
  notes?: string;
}

export interface RecognitionResult {
  structured: RecognizedWineFields;
  rawResponse: string;
}

export interface PairingCandidateBottle {
  id: string;
  name: string;
  producer: string | null;
  region: string | null;
  color: string;
  grapeVarieties: string[] | null;
  vintage: number | null;
  quantity: number;
}

export interface PairingBottleScore {
  bottleId: string;
  /** 1-10, deliberately spread out (9/7/6 rather than 8/8/8) so the ranking
   *  is visible, not just the order. */
  score: number;
  reasoning: string;
}

/** A wine to look for in a shop -- described by style/grape/region rather
 *  than a specific label, since it's not something the household owns. */
export interface PairingShoppingSuggestion {
  name: string;
  color: 'red' | 'white' | 'rose' | 'sparkling' | 'sweet' | 'fortified';
  region?: string;
  grapeVarieties?: string[];
  score: number;
  reasoning: string;
}

export interface PairingStructured {
  /** Up to 3, best first, drawn only from the candidates actually passed
   *  in (the household's own cellar). */
  cellarSuggestions: PairingBottleScore[];
  /** Up to 3, best first, independent of the cellar -- wines worth buying
   *  for this dish. */
  shoppingSuggestions: PairingShoppingSuggestion[];
}

export interface PairingResult {
  structured: PairingStructured;
  rawResponse: string;
}

export interface FoodPairingStructured {
  suggestedDishes: string[];
  reasoning: string;
}

export interface FoodPairingResult {
  structured: FoodPairingStructured;
  rawResponse: string;
}

/** Like FoodPairingStructured but a full recipe idea rather than a short
 *  dish name -- used for "apogée" (drinking window) alerts, where a more
 *  elaborate suggestion is worth the extra tokens since it's sent rarely
 *  (at most twice per bottle's lifetime, not on every lookup). */
export interface RecipeSuggestionStructured {
  recipeTitle: string;
  recipeDescription: string;
  reasoning: string;
}

export interface RecipeSuggestionResult {
  structured: RecipeSuggestionStructured;
  rawResponse: string;
}

export interface AIProviderClient {
  recognizeLabel(
    imageBase64: string,
    mimeType: string,
  ): Promise<RecognitionResult>;
  suggestPairing(
    dish: string,
    candidates: PairingCandidateBottle[],
  ): Promise<PairingResult>;
  /** Reverse of suggestPairing: given one specific bottle, suggest dishes that pair well with it. */
  suggestFoodForBottle(bottle: PairingCandidateBottle): Promise<FoodPairingResult>;
  /** Like suggestFoodForBottle but a full recipe idea (title + method) rather than a short dish name. */
  suggestRecipeForBottle(bottle: PairingCandidateBottle): Promise<RecipeSuggestionResult>;
}
