import { Body, Controller, Delete, Param, Post } from '@nestjs/common';
import { NotificationsService } from './notifications.service.js';
import { RegisterDeviceDto } from './dto/register-device.dto.js';
import { CurrentUser } from '../common/decorators/current-user.decorator.js';
import type { AuthenticatedUser } from '../auth/types.js';

@Controller('notifications/devices')
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  /** Called after login and whenever Firebase hands the app a new/rotated
   *  token -- registering is an upsert (by token), safe to call repeatedly. */
  @Post()
  register(@CurrentUser() user: AuthenticatedUser, @Body() dto: RegisterDeviceDto) {
    return this.notificationsService.registerDevice(user.id, user.householdId, dto);
  }

  /** Called on logout so a shared/reset device stops receiving this
   *  household's pushes. */
  @Delete(':token')
  unregister(@Param('token') token: string) {
    return this.notificationsService.unregisterDevice(token);
  }

  /** Sends an immediate test push to every device registered for the
   *  caller's household -- lets the user verify the whole chain (Firebase
   *  config on the backend + real google-services.json on the phone) works,
   *  without waiting for the daily 9am cron or a bottle that actually
   *  qualifies for a reminder. The returned summary (configured/deviceCount/
   *  successCount) is enough for the app to explain exactly what to fix if
   *  nothing arrives. */
  @Post('test')
  sendTest(@CurrentUser() user: AuthenticatedUser) {
    return this.notificationsService.sendToHousehold(user.householdId, {
      title: 'Notification de test',
      body: 'Si tu vois ceci, les notifications push fonctionnent.',
      data: { type: 'test' },
    });
  }
}
