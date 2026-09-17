import { afterAll, describe, expect, it } from 'vitest';
import { ConfigService } from '@nestjs/config';
import { NotificationsService } from './notifications.service.js';
import { createTestDb, createTestHousehold } from '../test-utils/test-db.js';

describe('NotificationsService', () => {
  const { db, pool } = createTestDb();
  // No FIREBASE_SERVICE_ACCOUNT_JSON on purpose -- this is the "not
  // configured yet" state every fresh deployment starts in, and sends must
  // degrade to a safe no-op rather than throwing.
  const notificationsService = new NotificationsService(db, new ConfigService({}));

  afterAll(async () => {
    await pool.end();
  });

  it('registers a device token and upserts it on re-registration under the same or a different user', async () => {
    const { household, user } = await createTestHousehold(db, 'DeviceReg');
    const token = `token-${crypto.randomUUID()}`;

    await notificationsService.registerDevice(user.id, household.id, { token });
    // Re-registering the same token (e.g. app restart) must not conflict.
    await expect(
      notificationsService.registerDevice(user.id, household.id, { token, platform: 'android' }),
    ).resolves.toEqual({ registered: true });
  });

  it('unregisters a device token without throwing when it does not exist', async () => {
    await expect(
      notificationsService.unregisterDevice(`unknown-${crypto.randomUUID()}`),
    ).resolves.toEqual({ unregistered: true });
  });

  it('sendToHousehold is a no-op when Firebase is not configured', async () => {
    const { household } = await createTestHousehold(db, 'NoFirebase');
    await expect(
      notificationsService.sendToHousehold(household.id, { title: 'Test', body: 'Test' }),
    ).resolves.toBeUndefined();
  });
});
