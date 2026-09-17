import { Inject, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { type App, cert, getApp, getApps, initializeApp } from 'firebase-admin/app';
import { getMessaging, type SendResponse } from 'firebase-admin/messaging';
import { eq, inArray } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { deviceTokens } from '../db/schema.js';
import type { RegisterDeviceDto } from './dto/register-device.dto.js';

export interface PushNotification {
  title: string;
  body: string;
  /** Extra data delivered alongside the notification (e.g. { type: 'turn-reminder' })
   *  so the app can navigate somewhere relevant when tapped, later. */
  data?: Record<string, string>;
}

/**
 * Thin wrapper around firebase-admin: registers/unregisters a household
 * member's device token and sends push notifications to every device of a
 * household (quart de tour reminders, apogée/drinking-window alerts).
 *
 * Deliberately tolerant of missing configuration: a self-hosted deployment
 * that hasn't set up a Firebase project yet (or a test/CI run) should still
 * boot and work normally -- sends just become no-ops, logged once, rather
 * than throwing.
 */
@Injectable()
export class NotificationsService {
  private readonly logger = new Logger(NotificationsService.name);
  private app: App | null = null;
  private warnedMissingConfig = false;

  constructor(
    @Inject(DRIZZLE) private readonly db: DrizzleDb,
    private readonly configService: ConfigService,
  ) {}

  private getApp(): App | null {
    if (this.app) return this.app;

    const raw = this.configService.get<string>('FIREBASE_SERVICE_ACCOUNT_JSON');
    if (!raw) {
      if (!this.warnedMissingConfig) {
        this.logger.warn(
          "FIREBASE_SERVICE_ACCOUNT_JSON n'est pas configuré -- les notifications push (quart de tour, etc.) sont désactivées.",
        );
        this.warnedMissingConfig = true;
      }
      return null;
    }

    try {
      const serviceAccount = JSON.parse(raw);
      this.app = getApps().length ? getApp() : initializeApp({ credential: cert(serviceAccount) });
      return this.app;
    } catch (error) {
      this.logger.error(
        `FIREBASE_SERVICE_ACCOUNT_JSON invalide, notifications push désactivées : ${(error as Error).message}`,
      );
      return null;
    }
  }

  async registerDevice(userId: string, householdId: string, dto: RegisterDeviceDto) {
    await this.db
      .insert(deviceTokens)
      .values({
        userId,
        householdId,
        token: dto.token,
        platform: dto.platform ?? 'android',
      })
      .onConflictDoUpdate({
        target: deviceTokens.token,
        set: {
          userId,
          householdId,
          platform: dto.platform ?? 'android',
          updatedAt: new Date(),
        },
      });
    return { registered: true };
  }

  async unregisterDevice(token: string) {
    await this.db.delete(deviceTokens).where(eq(deviceTokens.token, token));
    return { unregistered: true };
  }

  /** Sends to every device token on file for the household. Silently does
   *  nothing if Firebase isn't configured or the household has no
   *  registered devices. Prunes tokens FCM reports as dead/unregistered. */
  async sendToHousehold(householdId: string, notification: PushNotification): Promise<void> {
    const app = this.getApp();
    if (!app) return;

    const tokens = await this.db
      .select({ token: deviceTokens.token })
      .from(deviceTokens)
      .where(eq(deviceTokens.householdId, householdId));

    if (tokens.length === 0) return;

    const response = await getMessaging(app).sendEachForMulticast({
      tokens: tokens.map((t) => t.token),
      notification: { title: notification.title, body: notification.body },
      data: notification.data,
    });

    const deadTokens: string[] = [];
    response.responses.forEach((result: SendResponse, index: number) => {
      if (!result.success) {
        const code = result.error?.code;
        if (
          code === 'messaging/registration-token-not-registered' ||
          code === 'messaging/invalid-registration-token'
        ) {
          deadTokens.push(tokens[index].token);
        } else {
          this.logger.warn(`Échec d'envoi push : ${code ?? result.error?.message}`);
        }
      }
    });

    if (deadTokens.length > 0) {
      await this.db.delete(deviceTokens).where(inArray(deviceTokens.token, deadTokens));
    }
  }
}
