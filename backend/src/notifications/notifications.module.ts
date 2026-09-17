import { Module } from '@nestjs/common';
import { NotificationsService } from './notifications.service.js';
import { NotificationsController } from './notifications.controller.js';
import { TurnReminderScheduler } from './turn-reminder.scheduler.js';
import { ApogeeReminderScheduler } from './apogee-reminder.scheduler.js';
import { BottleModule } from '../bottle/bottle.module.js';
import { PairingModule } from '../pairing/pairing.module.js';

@Module({
  imports: [BottleModule, PairingModule],
  providers: [NotificationsService, TurnReminderScheduler, ApogeeReminderScheduler],
  controllers: [NotificationsController],
  exports: [NotificationsService],
})
export class NotificationsModule {}
