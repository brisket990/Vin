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
}
