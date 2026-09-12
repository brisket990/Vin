import { Inject, Injectable } from '@nestjs/common';
import { and, desc, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, tastingNotes } from '../db/schema.js';

const APOGEE_HORIZON_YEARS = 2;

@Injectable()
export class DashboardService {
  constructor(@Inject(DRIZZLE) private readonly db: DrizzleDb) {}

  /** Bottles entering or past their drinking window within the given horizon. Reused by the apogée notification job. */
  async getUpcomingApogeeBottles(
    householdId: string,
    withinYears = APOGEE_HORIZON_YEARS,
  ) {
    const inCellar = await this.db
      .select()
      .from(bottles)
      .where(and(eq(bottles.householdId, householdId), eq(bottles.status, 'in_cellar')));

    const currentYear = new Date().getFullYear();
    return inCellar
      .filter(
        (b) => b.drinkUntilYear !== null && b.drinkUntilYear - currentYear <= withinYears,
      )
      .sort((a, b) => (a.drinkUntilYear ?? 0) - (b.drinkUntilYear ?? 0));
  }

  async getStats(householdId: string) {
    const inCellar = await this.db
      .select()
      .from(bottles)
      .where(and(eq(bottles.householdId, householdId), eq(bottles.status, 'in_cellar')));

    const totalBottles = inCellar.reduce((sum, b) => sum + b.quantity, 0);
    const totalValueCents = inCellar.reduce(
      (sum, b) => sum + (b.purchasePriceCents ?? 0) * b.quantity,
      0,
    );

    const byColor: Record<string, number> = {};
    for (const b of inCellar) {
      byColor[b.color] = (byColor[b.color] ?? 0) + b.quantity;
    }

    const upcomingApogee = await this.getUpcomingApogeeBottles(householdId);

    const recentTastings = await this.db
      .select({
        id: tastingNotes.id,
        bottleId: tastingNotes.bottleId,
        bottleName: bottles.name,
        rating: tastingNotes.rating,
        comment: tastingNotes.comment,
        consumedDate: tastingNotes.consumedDate,
      })
      .from(tastingNotes)
      .innerJoin(bottles, eq(bottles.id, tastingNotes.bottleId))
      .where(eq(bottles.householdId, householdId))
      .orderBy(desc(tastingNotes.consumedDate))
      .limit(5);

    return {
      totalBottles,
      totalValueCents,
      byColor,
      upcomingApogee,
      recentTastings,
    };
  }
}
