import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { BottleService } from '../bottle/bottle.service.js';
import { PairingService } from '../pairing/pairing.service.js';
import { NotificationsService } from './notifications.service.js';

type BottleRow = Awaited<ReturnType<BottleService['findNeedingApogeeStartReminder']>>[number];

const MAX_BODY_LENGTH = 500;

/**
 * Daily job behind "apogée" (drinking window) alerts: one push when a
 * bottle enters its recommended drinking window (drinkFromYear reached),
 * and one when it's reaching the last recommended year (drinkUntilYear
 * reached) -- each sent at most once ever per bottle (see
 * BottleService.findNeedingApogeeStartReminder/EndReminder). Unlike the
 * quart de tour reminder, this doesn't repeat: a wine sitting in its window
 * for years doesn't need nagging every day, just the one heads-up at each
 * edge.
 *
 * Each notification is enriched with a full recipe idea from the
 * household's configured AI provider (see PairingService.suggestRecipeForBottle) --
 * that's the whole point of the alert ("this wine is ready, here's what to
 * cook with it"), not just a bare reminder. If the AI call fails for any
 * reason (no provider configured, rate limit, ...), the push still goes
 * out without the recipe rather than being lost entirely.
 */
@Injectable()
export class ApogeeReminderScheduler {
  private readonly logger = new Logger(ApogeeReminderScheduler.name);

  constructor(
    private readonly bottleService: BottleService,
    private readonly pairingService: PairingService,
    private readonly notificationsService: NotificationsService,
  ) {}

  @Cron(CronExpression.EVERY_DAY_AT_9AM)
  async sendDueStartReminders(): Promise<void> {
    const householdIds = await this.bottleService.findHouseholdIdsNeedingApogeeStartReminder();

    for (const householdId of householdIds) {
      try {
        const bottles = await this.bottleService.findNeedingApogeeStartReminder(householdId);
        for (const bottle of bottles) {
          await this.sendOne(householdId, bottle, {
            type: 'apogee-start',
            title: 'Une bouteille entre dans sa fenêtre de dégustation',
            baseBody: `${bottle.name} est maintenant dans sa période de dégustation optimale.`,
          });
        }
        await this.bottleService.markApogeeStartReminderSent(bottles.map((b) => b.id));
      } catch (error) {
        this.logger.error(
          `Échec de l'envoi du rappel d'apogée (entrée) pour le foyer ${householdId} : ${(error as Error).message}`,
        );
      }
    }
  }

  @Cron(CronExpression.EVERY_DAY_AT_9AM)
  async sendDueEndReminders(): Promise<void> {
    const householdIds = await this.bottleService.findHouseholdIdsNeedingApogeeEndReminder();

    for (const householdId of householdIds) {
      try {
        const bottles = await this.bottleService.findNeedingApogeeEndReminder(householdId);
        for (const bottle of bottles) {
          await this.sendOne(householdId, bottle, {
            type: 'apogee-end',
            title: 'Dernière année pour profiter de cette bouteille',
            baseBody: `${bottle.name} termine bientôt sa période de dégustation recommandée.`,
          });
        }
        await this.bottleService.markApogeeEndReminderSent(bottles.map((b) => b.id));
      } catch (error) {
        this.logger.error(
          `Échec de l'envoi du rappel d'apogée (fin) pour le foyer ${householdId} : ${(error as Error).message}`,
        );
      }
    }
  }

  private async sendOne(
    householdId: string,
    bottle: BottleRow,
    opts: { type: 'apogee-start' | 'apogee-end'; title: string; baseBody: string },
  ): Promise<void> {
    let body = opts.baseBody;

    try {
      const { recipeTitle, recipeDescription } = await this.pairingService.suggestRecipeForBottle(
        householdId,
        bottle.id,
      );
      body = `${opts.baseBody} Idée pour l'accompagner : ${recipeTitle} — ${recipeDescription}`;
    } catch (error) {
      this.logger.warn(
        `Pas de suggestion de recette pour "${bottle.name}" (${bottle.id}) : ${(error as Error).message}`,
      );
    }

    if (body.length > MAX_BODY_LENGTH) {
      body = `${body.slice(0, MAX_BODY_LENGTH - 1)}…`;
    }

    await this.notificationsService.sendToHousehold(householdId, {
      title: opts.title,
      body,
      data: { type: opts.type, bottleId: bottle.id },
    });
  }
}
