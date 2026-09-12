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
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
