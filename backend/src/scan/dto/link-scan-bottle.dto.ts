import { IsUUID } from 'class-validator';

export class LinkScanBottleDto {
  @IsUUID()
  bottleId!: string;
}
