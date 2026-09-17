import { IsInt, Max, Min } from 'class-validator';

export class NextFreeLocationsDto {
  @IsInt()
  @Min(1)
  @Max(200)
  count!: number;
}
