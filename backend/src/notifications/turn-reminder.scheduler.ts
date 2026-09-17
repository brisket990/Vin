import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { BottleService } from '../bottle/bottle.service.js';
import { NotificationsService } from './notifications.service.js';

/**
 * Daily job behind the "quart de tour" reminder: a bottle stored lying down
 * under natural cork for a long time should be given a quarter turn every
 * few months so sediment/the cork don't always settle on the same side (see
 * BottleService.TURN_REMINDER_THRESHOLD_DAYS). Runs once a day, finds every
 * household with at least one newly-overdue bottle (respecting each
 * bottle's own reminder cooldown so it doesn't nag daily), and sends one
 * push per household summarizing how many bottles need attention.
 */
@Injectable()
export class TurnReminderScheduler {
  private readonly logger = new Logger(TurnReminderScheduler.name);

  constructor(
    private readonly bottleService: BottleService,
    private readonly notificationsService: NotificationsService,
  ) {}

  @Cron(CronExpression.EVERY_DAY_AT_9AM)
  async sendDueReminders(): Promise<void> {
    const householdIds = await this.bottleService.findHouseholdIdsNeedingTurnReminder();

    for (const householdId of householdIds) {
      try {
        const bottles = await this.bottleService.findNeedingTurnReminder(householdId);
        if (bottles.length === 0) continue;

        const body =
          bottles.length === 1
            ? `${bottles[0].name} attend son quart de tour depuis un moment.`
            : `${bottles.length} bouteilles attendent leur quart de tour depuis un moment.`;

        await this.notificationsService.sendToHousehold(householdId, {
          title: 'Un petit tour pour tes bouteilles',
          body,
          data: { type: 'turn-reminder' },
        });

        await this.bottleService.markTurnReminderSent(bottles.map((b) => b.id));
      } catch (error) {
        this.logger.error(
          `Échec de l'envoi du rappel quart de tour pour le foyer ${householdId} : ${(error as Error).message}`,
        );
      }
    }
  }
}
