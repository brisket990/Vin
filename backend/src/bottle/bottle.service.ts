import {
  BadRequestException,
  Inject,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { and, eq, gte, ilike, inArray, lte, or, sql, type SQL } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, cellarLocations, cellarUnits, tastingNotes } from '../db/schema.js';
import type { CreateBottleDto } from './dto/create-bottle.dto.js';
import type { UpdateBottleDto } from './dto/update-bottle.dto.js';
import type { QueryBottlesDto } from './dto/query-bottles.dto.js';
import type { ConsumeBottleDto } from './dto/consume-bottle.dto.js';

/** Classic advice for a bottle aged a long time lying down under natural
 *  cork: give it a quarter turn every few months so sediment/the cork don't
 *  always settle on the same side. */
export const TURN_REMINDER_THRESHOLD_DAYS = 90;

/** Minimum gap between two push reminders for the *same* bottle, so the
 *  daily job doesn't nag every single day once a bottle is overdue. */
export const TURN_REMINDER_COOLDOWN_DAYS = 7;

@Injectable()
export class BottleService {
  constructor(@Inject(DRIZZLE) private readonly db: DrizzleDb) {}

  private async assertLocationAvailable(
    householdId: string,
    locationId: string,
    excludeBottleId?: string,
  ) {
    const [location] = await this.db
      .select({ id: cellarLocations.id })
      .from(cellarLocations)
      .innerJoin(cellarUnits, eq(cellarUnits.id, cellarLocations.unitId))
      .where(
        and(
          eq(cellarLocations.id, locationId),
          eq(cellarUnits.householdId, householdId),
        ),
      )
      .limit(1);

    if (!location) {
      throw new BadRequestException(
        "Cet emplacement n'existe pas dans ta cave.",
      );
    }

    const occupantConditions = [
      eq(bottles.locationId, locationId),
      eq(bottles.status, 'in_cellar'),
    ];
    const [occupant] = await this.db
      .select({ id: bottles.id })
      .from(bottles)
      .where(and(...occupantConditions))
      .limit(1);

    if (occupant && occupant.id !== excludeBottleId) {
      throw new BadRequestException('Cet emplacement est déjà occupé.');
    }
  }

  async create(householdId: string, dto: CreateBottleDto) {
    if (dto.locationId) {
      await this.assertLocationAvailable(householdId, dto.locationId);
    }

    const [bottle] = await this.db
      .insert(bottles)
      .values({
        householdId,
        name: dto.name,
        producer: dto.producer,
        region: dto.region,
        appellation: dto.appellation,
        grapeVarieties: dto.grapeVarieties,
        vintage: dto.vintage,
        color: dto.color,
        quantity: dto.quantity ?? 1,
        purchasePriceCents: dto.purchasePriceCents,
        purchaseDate: dto.purchaseDate,
        drinkFromYear: dto.drinkFromYear,
        drinkUntilYear: dto.drinkUntilYear,
        locationId: dto.locationId,
        labelPhotoUrl: dto.labelPhotoUrl,
        notes: dto.notes,
        tastingNose: dto.tastingNose,
        tastingPalate: dto.tastingPalate,
        tastingSweetness: dto.tastingSweetness,
      })
      .returning();

    return bottle;
  }

  async findAll(householdId: string, query: QueryBottlesDto) {
    const conditions: SQL[] = [eq(bottles.householdId, householdId)];

    if (query.color) conditions.push(eq(bottles.color, query.color));
    if (query.status) conditions.push(eq(bottles.status, query.status));
    if (query.region) conditions.push(ilike(bottles.region, `%${query.region}%`));
    if (query.vintageMin !== undefined)
      conditions.push(gte(bottles.vintage, query.vintageMin));
    if (query.vintageMax !== undefined)
      conditions.push(lte(bottles.vintage, query.vintageMax));
    if (query.priceMaxCents !== undefined)
      conditions.push(lte(bottles.purchasePriceCents, query.priceMaxCents));
    if (query.search) {
      const pattern = `%${query.search}%`;
      const searchCondition = or(
        ilike(bottles.name, pattern),
        ilike(bottles.producer, pattern),
        ilike(bottles.appellation, pattern),
      );
      if (searchCondition) conditions.push(searchCondition);
    }

    const results = await this.db
      .select()
      .from(bottles)
      .where(and(...conditions))
      .orderBy(bottles.createdAt);

    // grape is matched in-memory since it's a text[] column (simple substring
    // match across the array, case-insensitive).
    if (query.grape) {
      const needle = query.grape.toLowerCase();
      return results.filter((b) =>
        (b.grapeVarieties ?? []).some((g) => g.toLowerCase().includes(needle)),
      );
    }

    return results;
  }

  private async getOwnedBottleOrThrow(householdId: string, id: string) {
    const [bottle] = await this.db
      .select()
      .from(bottles)
      .where(and(eq(bottles.id, id), eq(bottles.householdId, householdId)))
      .limit(1);

    if (!bottle) throw new NotFoundException('Bouteille introuvable.');
    return bottle;
  }

  async findOne(householdId: string, id: string) {
    return this.getOwnedBottleOrThrow(householdId, id);
  }

  async update(householdId: string, id: string, dto: UpdateBottleDto) {
    await this.getOwnedBottleOrThrow(householdId, id);

    if (dto.locationId) {
      await this.assertLocationAvailable(householdId, dto.locationId, id);
    }

    const [bottle] = await this.db
      .update(bottles)
      .set({ ...dto, updatedAt: new Date() })
      .where(and(eq(bottles.id, id), eq(bottles.householdId, householdId)))
      .returning();

    return bottle;
  }

  async remove(householdId: string, id: string) {
    await this.getOwnedBottleOrThrow(householdId, id);
    await this.db
      .delete(bottles)
      .where(and(eq(bottles.id, id), eq(bottles.householdId, householdId)));
    return { deleted: true };
  }

  /**
   * Marks (part of) a bottle as drunk: decrements quantity, records a
   * tasting note, and frees the cellar slot once the last unit is gone.
   */
  async consume(
    householdId: string,
    id: string,
    userId: string,
    dto: ConsumeBottleDto,
  ) {
    const bottle = await this.getOwnedBottleOrThrow(householdId, id);
    const consumedQuantity = dto.quantity ?? 1;

    if (bottle.status !== 'in_cellar') {
      throw new BadRequestException('Cette bouteille est déjà consommée.');
    }
    if (consumedQuantity > bottle.quantity) {
      throw new BadRequestException(
        'Quantité consommée supérieure à la quantité en cave.',
      );
    }

    return this.db.transaction(async (tx) => {
      const remaining = bottle.quantity - consumedQuantity;
      const [updated] = await tx
        .update(bottles)
        .set({
          quantity: remaining,
          status: remaining > 0 ? 'in_cellar' : 'consumed',
          locationId: remaining > 0 ? bottle.locationId : null,
          updatedAt: new Date(),
        })
        .where(eq(bottles.id, id))
        .returning();

      const [note] = await tx
        .insert(tastingNotes)
        .values({
          bottleId: id,
          userId,
          rating: dto.rating,
          comment: dto.comment,
          consumedDate: dto.consumedDate ?? new Date().toISOString().slice(0, 10),
        })
        .returning();

      return { bottle: updated, tastingNote: note };
    });
  }

  /** Records that a bottle was just given its quarter turn, resetting both
   *  the overdue baseline and the reminder cooldown. */
  async turn(householdId: string, id: string) {
    await this.getOwnedBottleOrThrow(householdId, id);
    const [bottle] = await this.db
      .update(bottles)
      .set({ lastTurnedAt: new Date(), lastTurnReminderSentAt: null, updatedAt: new Date() })
      .where(and(eq(bottles.id, id), eq(bottles.householdId, householdId)))
      .returning();
    return bottle;
  }

  /** Every in-cellar bottle overdue for a quarter turn (see
   *  TURN_REMINDER_THRESHOLD_DAYS) -- used by the app to show which bottles
   *  need attention, regardless of whether a push reminder was already sent. */
  async findNeedingTurn(householdId: string) {
    return this.db
      .select()
      .from(bottles)
      .where(
        and(
          eq(bottles.householdId, householdId),
          eq(bottles.status, 'in_cellar'),
          sql`coalesce(${bottles.lastTurnedAt}, ${bottles.createdAt}) <= now() - (${TURN_REMINDER_THRESHOLD_DAYS} * interval '1 day')`,
        ),
      )
      .orderBy(bottles.createdAt);
  }

  /** Overdue bottles that additionally haven't had a push reminder sent
   *  recently (see TURN_REMINDER_COOLDOWN_DAYS) -- used by the daily job so
   *  it doesn't re-notify about the same bottle every day. */
  async findNeedingTurnReminder(householdId: string) {
    return this.db
      .select()
      .from(bottles)
      .where(
        and(
          eq(bottles.householdId, householdId),
          eq(bottles.status, 'in_cellar'),
          sql`coalesce(${bottles.lastTurnedAt}, ${bottles.createdAt}) <= now() - (${TURN_REMINDER_THRESHOLD_DAYS} * interval '1 day')`,
          sql`(${bottles.lastTurnReminderSentAt} IS NULL OR ${bottles.lastTurnReminderSentAt} <= now() - (${TURN_REMINDER_COOLDOWN_DAYS} * interval '1 day'))`,
        ),
      )
      .orderBy(bottles.createdAt);
  }

  /** Distinct household ids that currently have at least one bottle due a
   *  push reminder -- lets the daily job avoid iterating every household in
   *  the database when most have nothing overdue. */
  async findHouseholdIdsNeedingTurnReminder(): Promise<string[]> {
    const rows = await this.db
      .selectDistinct({ householdId: bottles.householdId })
      .from(bottles)
      .where(
        and(
          eq(bottles.status, 'in_cellar'),
          sql`coalesce(${bottles.lastTurnedAt}, ${bottles.createdAt}) <= now() - (${TURN_REMINDER_THRESHOLD_DAYS} * interval '1 day')`,
          sql`(${bottles.lastTurnReminderSentAt} IS NULL OR ${bottles.lastTurnReminderSentAt} <= now() - (${TURN_REMINDER_COOLDOWN_DAYS} * interval '1 day'))`,
        ),
      );
    return rows.map((r) => r.householdId);
  }

  async markTurnReminderSent(bottleIds: string[]) {
    if (bottleIds.length === 0) return;
    await this.db
      .update(bottles)
      .set({ lastTurnReminderSentAt: new Date() })
      .where(inArray(bottles.id, bottleIds));
  }

  /** In-cellar bottles whose drinking window just started (current year has
   *  reached drinkFromYear) and that haven't had their one-time "entering
   *  the window" push sent yet. Bottles without a drinkFromYear never
   *  match. */
  async findNeedingApogeeStartReminder(householdId: string) {
    return this.db
      .select()
      .from(bottles)
      .where(
        and(
          eq(bottles.householdId, householdId),
          eq(bottles.status, 'in_cellar'),
          sql`${bottles.drinkFromYear} IS NOT NULL`,
          sql`${bottles.drinkFromYear} <= extract(year from now())`,
          sql`${bottles.apogeeStartReminderSentAt} IS NULL`,
        ),
      )
      .orderBy(bottles.createdAt);
  }

  /** In-cellar bottles reaching the last recommended year of their drinking
   *  window (current year has reached drinkUntilYear) and that haven't had
   *  their one-time "last chance" push sent yet. Bottles without a
   *  drinkUntilYear never match. */
  async findNeedingApogeeEndReminder(householdId: string) {
    return this.db
      .select()
      .from(bottles)
      .where(
        and(
          eq(bottles.householdId, householdId),
          eq(bottles.status, 'in_cellar'),
          sql`${bottles.drinkUntilYear} IS NOT NULL`,
          sql`${bottles.drinkUntilYear} <= extract(year from now())`,
          sql`${bottles.apogeeEndReminderSentAt} IS NULL`,
        ),
      )
      .orderBy(bottles.createdAt);
  }

  /** Distinct household ids with at least one bottle due its one-time
   *  "entering the window" push -- lets the daily job skip households with
   *  nothing to report. */
  async findHouseholdIdsNeedingApogeeStartReminder(): Promise<string[]> {
    const rows = await this.db
      .selectDistinct({ householdId: bottles.householdId })
      .from(bottles)
      .where(
        and(
          eq(bottles.status, 'in_cellar'),
          sql`${bottles.drinkFromYear} IS NOT NULL`,
          sql`${bottles.drinkFromYear} <= extract(year from now())`,
          sql`${bottles.apogeeStartReminderSentAt} IS NULL`,
        ),
      );
    return rows.map((r) => r.householdId);
  }

  /** Same as findHouseholdIdsNeedingApogeeStartReminder but for the "last
   *  chance" push. */
  async findHouseholdIdsNeedingApogeeEndReminder(): Promise<string[]> {
    const rows = await this.db
      .selectDistinct({ householdId: bottles.householdId })
      .from(bottles)
      .where(
        and(
          eq(bottles.status, 'in_cellar'),
          sql`${bottles.drinkUntilYear} IS NOT NULL`,
          sql`${bottles.drinkUntilYear} <= extract(year from now())`,
          sql`${bottles.apogeeEndReminderSentAt} IS NULL`,
        ),
      );
    return rows.map((r) => r.householdId);
  }

  async markApogeeStartReminderSent(bottleIds: string[]) {
    if (bottleIds.length === 0) return;
    await this.db
      .update(bottles)
      .set({ apogeeStartReminderSentAt: new Date() })
      .where(inArray(bottles.id, bottleIds));
  }

  async markApogeeEndReminderSent(bottleIds: string[]) {
    if (bottleIds.length === 0) return;
    await this.db
      .update(bottles)
      .set({ apogeeEndReminderSentAt: new Date() })
      .where(inArray(bottles.id, bottleIds));
  }
}
