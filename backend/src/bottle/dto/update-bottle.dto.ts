import { PartialType } from '@nestjs/mapped-types';
import { CreateBottleDto } from './create-bottle.dto.js';

export class UpdateBottleDto extends PartialType(CreateBottleDto) {}
