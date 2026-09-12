import { IsInt, IsString, Max, Min, MinLength } from 'class-validator';

export class CreateCellarUnitDto {
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
}
