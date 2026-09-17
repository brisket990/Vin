import { IsIn, IsOptional, IsString, MinLength } from 'class-validator';
import { devicePlatformValues } from '../../db/schema.js';

export class RegisterDeviceDto {
  @IsString()
  @MinLength(1)
  token!: string;

  @IsOptional()
  @IsIn(devicePlatformValues)
  platform?: (typeof devicePlatformValues)[number];
}
