export interface RecognizedWineFields {
  name?: string;
  producer?: string;
  region?: string;
  appellation?: string;
  grapeVarieties?: string[];
  vintage?: number;
  color?: 'red' | 'white' | 'rose' | 'sparkling' | 'sweet' | 'fortified';
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

export interface PairingStructured {
  suggestedBottleIds: string[];
  reasoning: string;
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
}
