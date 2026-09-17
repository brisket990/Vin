import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ScheduleModule } from '@nestjs/schedule';
import { validateEnv } from './config/env.validation.js';
import { DrizzleModule } from './db/drizzle.module.js';
import { AppController } from './app.controller.js';
import { AppService } from './app.service.js';
import { AuthModule } from './auth/auth.module.js';
import { HouseholdModule } from './household/household.module.js';
import { CellarModule } from './cellar/cellar.module.js';
import { BottleModule } from './bottle/bottle.module.js';
import { AiProviderModule } from './ai-provider/ai-provider.module.js';
import { ScanModule } from './scan/scan.module.js';
import { PairingModule } from './pairing/pairing.module.js';
import { TastingModule } from './tasting/tasting.module.js';
import { WishlistModule } from './wishlist/wishlist.module.js';
import { DashboardModule } from './dashboard/dashboard.module.js';
import { ExportModule } from './export/export.module.js';
import { ImportModule } from './import/import.module.js';
import { NotificationsModule } from './notifications/notifications.module.js';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      validate: validateEnv,
    }),
    ScheduleModule.forRoot(),
    DrizzleModule,
    AuthModule,
    HouseholdModule,
    CellarModule,
    BottleModule,
    AiProviderModule,
    ScanModule,
    PairingModule,
    TastingModule,
    WishlistModule,
    DashboardModule,
    ExportModule,
    ImportModule,
    NotificationsModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
