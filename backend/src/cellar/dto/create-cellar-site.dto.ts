import { IsString, MinLength } from 'class-validator';

export class CreateCellarSiteDto {
  @IsString()
  @MinLength(1)
  name!: string;
}
