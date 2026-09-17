import { Injectable } from '@nestjs/common';
import { plainToInstance } from 'class-transformer';
import { validate, type ValidationError } from 'class-validator';
import { BottleService } from '../bottle/bottle.service.js';
import { CreateBottleDto } from '../bottle/dto/create-bottle.dto.js';
import { parseCsv } from './util/csv-parser.js';

/** Columns a row must have a value for -- everything else in
 *  ExportService's CSV_COLUMNS is optional on the way back in. */
const REQUIRED_COLUMNS = ['name', 'color'] as const;

export interface ImportRowError {
  /** 1-based line number, counting the header as line 1, so it matches what
   *  the user sees if they open the file in a spreadsheet app. */
  row: number;
  message: string;
}

export interface ImportCsvResult {
  imported: number;
  skippedConsumed: number;
  errors: ImportRowError[];
}

@Injectable()
export class ImportService {
  constructor(private readonly bottleService: BottleService) {}

  /**
   * Imports bottles from a CSV in the same column format ExportService
   * produces (see CSV_COLUMNS there) -- lets a household restore or merge a
   * previously exported cave, or one prepared by hand in a spreadsheet.
   *
   * Two deliberate simplifications for this first version:
   *  - Imported bottles are always created without a cellar location. The
   *    exported "location" column is just a free-text label (e.g. "R2-C2")
   *    that can collide across different casiers, so there's no reliable way
   *    to map it back to a real slot -- bottles land in "sans emplacement"
   *    and can be placed from the existing bottle-edit screen.
   *  - Rows whose "status" column is "consumed" are skipped rather than
   *    imported as if still in the cellar, so re-importing your own export
   *    doesn't resurrect already-drunk bottles.
   *
   * Never throws for a bad row -- each is validated independently and
   * reported in `errors`, so one typo doesn't abort the whole file.
   */
  async importCsv(householdId: string, csvText: string): Promise<ImportCsvResult> {
    const rows = parseCsv(csvText.replace(/^﻿/, ''));
    const result: ImportCsvResult = { imported: 0, skippedConsumed: 0, errors: [] };

    if (rows.length === 0) {
      result.errors.push({ row: 0, message: 'Fichier vide.' });
      return result;
    }

    const header = rows[0].map((h) => h.trim());
    for (const required of REQUIRED_COLUMNS) {
      if (!header.includes(required)) {
        result.errors.push({
          row: 0,
          message: `Colonne obligatoire manquante dans l'en-tête : "${required}".`,
        });
        return result;
      }
    }

    for (let i = 1; i < rows.length; i++) {
      const rowNumber = i + 1; // 1-based, header counted as line 1
      const raw = rows[i];
      if (raw.length === 1 && raw[0].trim() === '') continue; // blank line

      const record: Record<string, string> = {};
      header.forEach((col, idx) => {
        record[col] = raw[idx] ?? '';
      });
      const label = record.name?.trim() || 'Bouteille sans nom';

      if ((record.status ?? '').trim() === 'consumed') {
        result.skippedConsumed++;
        continue;
      }

      const dto = plainToInstance(CreateBottleDto, this.toBottlePlain(record));
      const violations = await validate(dto, { whitelist: true });
      if (violations.length > 0) {
        result.errors.push({
          row: rowNumber,
          message: `${label} : ${this.describeViolations(violations)}`,
        });
        continue;
      }

      try {
        await this.bottleService.create(householdId, dto);
        result.imported++;
      } catch (e) {
        result.errors.push({
          row: rowNumber,
          message: `${label} : ${(e as Error).message}`,
        });
      }
    }

    return result;
  }

  private toBottlePlain(record: Record<string, string>): Record<string, unknown> {
    const get = (col: string): string | undefined => {
      const value = record[col]?.trim();
      return value ? value : undefined;
    };
    const getInt = (col: string): number | undefined => {
      const value = get(col);
      if (value === undefined) return undefined;
      const parsed = Number.parseInt(value, 10);
      return Number.isNaN(parsed) ? undefined : parsed;
    };

    return {
      name: get('name'),
      producer: get('producer'),
      region: get('region'),
      appellation: get('appellation'),
      grapeVarieties: get('grapeVarieties')
        ?.split(';')
        .map((g) => g.trim())
        .filter(Boolean),
      vintage: getInt('vintage'),
      color: get('color'),
      quantity: getInt('quantity'),
      purchasePriceCents: getInt('purchasePriceCents'),
      purchaseDate: get('purchaseDate'),
      drinkFromYear: getInt('drinkFromYear'),
      drinkUntilYear: getInt('drinkUntilYear'),
      notes: get('notes'),
    };
  }

  private describeViolations(violations: ValidationError[]): string {
    const messages = violations.flatMap((v) => Object.values(v.constraints ?? {}));
    return messages.length > 0 ? messages.join('; ') : 'valeurs invalides.';
  }
}
