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
    ).resolves.toEqual({ configured: false, deviceCount: 0, successCount: 0 });
  });

  // Coolify (and some other Docker-based hosts) inject every environment
  // variable into the image build as a raw, unquoted Dockerfile ARG line --
  // this key's JSON (quotes, braces, embedded newlines in `private_key`)
  // breaks that line's syntax and fails the build outright. Base64 avoids
  // the whole class of parsing failures (see NotificationsService.decodeServiceAccountJson),
  // so it must be accepted transparently alongside plain JSON.
  it('accepts a base64-encoded FIREBASE_SERVICE_ACCOUNT_JSON', async () => {
    const { household } = await createTestHousehold(db, 'Base64Cfg');
    // Structurally valid JSON but not a real Firebase credential -- getApp()
    // still degrades to a safe no-op (configured: false) once cert()/
    // initializeApp() rejects it for missing required claims, same as any
    // other invalid config. The point of this test is that the base64
    // decoding + JSON.parse succeed rather than mis-parsing the value.
    const encoded = Buffer.from(JSON.stringify({ not: 'a real service account' })).toString(
      'base64',
    );
    const service = new NotificationsService(
      db,
      new ConfigService({ FIREBASE_SERVICE_ACCOUNT_JSON: encoded }),
    );
    await expect(
      service.sendToHousehold(household.id, { title: 'Test', body: 'Test' }),
    ).resolves.toEqual({ configured: false, deviceCount: 0, successCount: 0 });
  });
});
