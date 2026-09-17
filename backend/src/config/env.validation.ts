import { plainToInstance } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  Min,
  validateSync,
} from 'class-validator';

class EnvironmentVariables {
  @IsIn(['development', 'production', 'test'])
  @IsOptional()
  NODE_ENV?: string;

  @IsInt()
  @Min(1)
  @Max(65535)
  @IsOptional()
  PORT?: number;

  @IsString()
  @IsNotEmpty()
  DATABASE_URL!: string;

  @IsString()
  @IsNotEmpty()
  JWT_SECRET!: string;

  @IsString()
  @IsOptional()
  JWT_EXPIRES_IN?: string;

  // 32-byte key (base64 or hex) used to encrypt AI provider API keys at rest (AES-256-GCM).
  @IsString()
  @IsNotEmpty()
  API_KEY_ENCRYPTION_SECRET!: string;

  @IsString()
  @IsOptional()
  STORAGE_DIR?: string;

  // Full JSON content of a Firebase service account key (Firebase console ->
  // Project settings -> Service accounts -> Generate new private key), used
  // to send push notifications (quart de tour reminders, later apogée
  // alerts). Optional: when unset, NotificationsService logs a warning once
  // and every send becomes a no-op, so the rest of the app (and tests) work
  // fine without it configured.
  @IsString()
  @IsOptional()
  FIREBASE_SERVICE_ACCOUNT_JSON?: string;
}

export function validateEnv(config: Record<string, unknown>) {
  const validated = plainToInstance(EnvironmentVariables, config, {
    enableImplicitConversion: true,
  });
  const errors = validateSync(validated, {
    skipMissingProperties: false,
  });

  if (errors.length > 0) {
    throw new Error(
      `Configuration invalide:\n${errors
        .map((error) => Object.values(error.constraints ?? {}).join(', '))
        .join('\n')}`,
    );
  }

  return validated;
}
