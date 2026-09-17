import { IsString, MinLength } from 'class-validator';

export class UpdateCellarSiteDto {
  @IsString()
  @MinLength(1)
  name!: string;
}
