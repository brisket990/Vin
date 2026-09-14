import {
  BadRequestException,
  Inject,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { and, desc, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, pairingSuggestions } from '../db/schema.js';
import { AiProviderService } from '../ai-provider/ai-provider.service.js';
import type { PairingCandidateBottle } from '../ai-provider/types.js';
import type { CreatePairingDto } from './dto/create-pairing.dto.js';

@Injectable()
export class PairingService {
  constructor(
    @Inject(DRIZZLE) private readonly db: DrizzleDb,
    private readonly aiProviderService: AiProviderService,
  ) {}

  async suggest(householdId: string, dto: CreatePairingDto) {
    const candidates = await this.db
      .select()
      .from(bottles)
      .where(and(eq(bottles.householdId, householdId), eq(bottles.status, 'in_cellar')));

    if (candidates.length === 0) {
      throw new BadRequestException(
        "Ta cave ne contient aucune bouteille disponible pour l'instant.",
      );
    }

    const candidateSummaries: PairingCandidateBottle[] = candidates.map((b) => ({
      id: b.id,
      name: b.name,
      producer: b.producer,
      region: b.region,
      color: b.color,
      grapeVarieties: b.grapeVarieties,
      vintage: b.vintage,
      quantity: b.quantity,
    }));

    const { provider, client } = await this.aiProviderService.getClientForUsage(
      householdId,
      'pairing',
      dto.provider,
    );

    const { structured, rawResponse } = await client.suggestPairing(
      dto.dishDescription,
      candidateSummaries,
    );

    // Defensive filter: only keep ids the AI actually had available, in case
    // it hallucinates one that isn't in the cellar.
    const validIds = new Set(candidates.map((c) => c.id));
    const suggestedBottleIds = (structured.suggestedBottleIds ?? []).filter((id) =>
      validIds.has(id),
    );

    const [saved] = await this.db
      .insert(pairingSuggestions)
      .values({
        householdId,
        dishDescription: dto.dishDescription,
        suggestedBottleIds,
        provider,
        rawResponse,
        structuredFields: structured,
      })
      .returning();

    return {
      ...saved,
      suggestedBottles: candidates.filter((c) => suggestedBottleIds.includes(c.id)),
    };
  }

  async findAll(householdId: string) {
    return this.db
      .select()
      .from(pairingSuggestions)
      .where(eq(pairingSuggestions.householdId, householdId))
      .orderBy(desc(pairingSuggestions.createdAt));
  }

  async findOne(householdId: string, id: string) {
    const [result] = await this.db
      .select()
      .from(pairingSuggestions)
      .where(
        and(eq(pairingSuggestions.id, id), eq(pairingSuggestions.householdId, householdId)),
      )
      .limit(1);

    if (!result) throw new NotFoundException('Suggestion introuvable.');
    return result;
  }

  /**
   * Reverse of suggest(): given one specific bottle (rather than a dish),
   * asks the AI directly what food pairs well with it. Stateless for now --
   * unlike suggest() this isn't persisted, since it's a quick lookup rather
   * than a decision the household is tracking.
   */
  async suggestForBottle(householdId: string, bottleId: string, provider?: string) {
    const [bottle] = await this.db
      .select()
      .from(bottles)
      .where(and(eq(bottles.id, bottleId), eq(bottles.householdId, householdId)))
      .limit(1);

    if (!bottle) throw new NotFoundException('Bouteille introuvable.');

    const candidate: PairingCandidateBottle = {
      id: bottle.id,
      name: bottle.name,
      producer: bottle.producer,
      region: bottle.region,
      color: bottle.color,
      grapeVarieties: bottle.grapeVarieties,
      vintage: bottle.vintage,
      quantity: bottle.quantity,
    };

    const { provider: usedProvider, client } = await this.aiProviderService.getClientForUsage(
      householdId,
      'pairing',
      provider,
    );

    const { structured, rawResponse } = await client.suggestFoodForBottle(candidate);

    return {
      bottleId: bottle.id,
      provider: usedProvider,
      suggestedDishes: structured.suggestedDishes ?? [],
      reasoning: structured.reasoning,
      rawResponse,
    };
  }
}
