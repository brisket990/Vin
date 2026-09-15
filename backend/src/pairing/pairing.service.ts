import {
  BadRequestException,
  Inject,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { and, desc, eq, inArray } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, pairingSuggestions } from '../db/schema.js';
import { AiProviderService } from '../ai-provider/ai-provider.service.js';
import type {
  PairingBottleScore,
  PairingCandidateBottle,
  PairingShoppingSuggestion,
} from '../ai-provider/types.js';
import type { CreatePairingDto } from './dto/create-pairing.dto.js';

type BottleRow = typeof bottles.$inferSelect;
type PairingRow = typeof pairingSuggestions.$inferSelect;

const MAX_SUGGESTIONS = 3;

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
    const cellarSuggestions = (structured.cellarSuggestions ?? [])
      .filter((s) => validIds.has(s.bottleId))
      .slice(0, MAX_SUGGESTIONS);
    const shoppingSuggestions = (structured.shoppingSuggestions ?? []).slice(
      0,
      MAX_SUGGESTIONS,
    );

    const [saved] = await this.db
      .insert(pairingSuggestions)
      .values({
        householdId,
        dishDescription: dto.dishDescription,
        suggestedBottleIds: cellarSuggestions.map((s) => s.bottleId),
        provider,
        rawResponse,
        structuredFields: { cellarSuggestions, shoppingSuggestions },
      })
      .returning();

    const bottlesById = new Map(candidates.map((b) => [b.id, b]));
    return this.toResponse(saved, bottlesById);
  }

  async findAll(householdId: string) {
    const rows = await this.db
      .select()
      .from(pairingSuggestions)
      .where(eq(pairingSuggestions.householdId, householdId))
      .orderBy(desc(pairingSuggestions.createdAt));

    const bottlesById = await this.loadReferencedBottles(householdId, rows);
    return rows.map((row) => this.toResponse(row, bottlesById));
  }

  async findOne(householdId: string, id: string) {
    const [row] = await this.db
      .select()
      .from(pairingSuggestions)
      .where(
        and(eq(pairingSuggestions.id, id), eq(pairingSuggestions.householdId, householdId)),
      )
      .limit(1);

    if (!row) throw new NotFoundException('Suggestion introuvable.');

    const bottlesById = await this.loadReferencedBottles(householdId, [row]);
    return this.toResponse(row, bottlesById);
  }

  private async loadReferencedBottles(householdId: string, rows: PairingRow[]) {
    const ids = new Set<string>();
    for (const row of rows) {
      for (const id of row.suggestedBottleIds ?? []) ids.add(id);
    }
    if (ids.size === 0) return new Map<string, BottleRow>();

    const found = await this.db
      .select()
      .from(bottles)
      .where(and(eq(bottles.householdId, householdId), inArray(bottles.id, Array.from(ids))));

    return new Map(found.map((b) => [b.id, b]));
  }

  /**
   * Normalizes a stored row into the shape the app renders: up to 3 cellar
   * picks (each carrying the full bottle plus its score/reasoning) and up
   * to 3 shopping suggestions. Falls back gracefully for rows saved before
   * this shape existed (old { suggestedBottleIds, reasoning } structure --
   * shown without a per-bottle score rather than dropped).
   */
  private toResponse(row: PairingRow, bottlesById: Map<string, BottleRow>) {
    const structured = row.structuredFields as
      | { cellarSuggestions?: PairingBottleScore[]; shoppingSuggestions?: PairingShoppingSuggestion[] }
      | { reasoning?: string }
      | null
      | undefined;

    const cellarSuggestionsRaw: PairingBottleScore[] = Array.isArray(
      (structured as { cellarSuggestions?: PairingBottleScore[] })?.cellarSuggestions,
    )
      ? (structured as { cellarSuggestions: PairingBottleScore[] }).cellarSuggestions
      : (row.suggestedBottleIds ?? []).map((bottleId) => ({
          bottleId,
          score: null as unknown as number,
          reasoning: (structured as { reasoning?: string })?.reasoning ?? '',
        }));

    const cellarSuggestions = cellarSuggestionsRaw
      .filter((s) => bottlesById.has(s.bottleId))
      .slice(0, MAX_SUGGESTIONS)
      .map((s) => ({
        bottle: bottlesById.get(s.bottleId)!,
        score: s.score ?? null,
        reasoning: s.reasoning ?? '',
      }));

    const shoppingSuggestions = Array.isArray(
      (structured as { shoppingSuggestions?: PairingShoppingSuggestion[] })?.shoppingSuggestions,
    )
      ? (structured as { shoppingSuggestions: PairingShoppingSuggestion[] }).shoppingSuggestions.slice(
          0,
          MAX_SUGGESTIONS,
        )
      : [];

    return {
      id: row.id,
      householdId: row.householdId,
      dishDescription: row.dishDescription,
      provider: row.provider,
      rawResponse: row.rawResponse,
      createdAt: row.createdAt,
      cellarSuggestions,
      shoppingSuggestions,
    };
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
