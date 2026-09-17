import { IsIn, IsInt, IsOptional, IsString, IsUUID, Max, Min, MinLength } from 'class-validator';
import { wineColorValues } from '../../db/schema.js';

export class CreateCellarUnitDto {
  /** Which cave (physical location) this casier belongs to -- required now
   *  that a household can have more than one (see CellarSite). */
  @IsUUID()
  siteId!: string;

  @IsString()
  @MinLength(1)
  name!: string;

  @IsInt()
  @Min(1)
  @Max(200)
  rowCount!: number;

  @IsInt()
  @Min(1)
  @Max(200)
  columnCount!: number;

  /** Optional dedicated color for this unit (e.g. a household with several
   *  units keeps "Cave rouges" separate from "Cave blancs") -- see
   *  CellarService.suggestAcrossUnits. Omitted/null means mixed, no
   *  preference. */
  @IsOptional()
  @IsIn(wineColorValues)
  preferredColor?: (typeof wineColorValues)[number];
}
