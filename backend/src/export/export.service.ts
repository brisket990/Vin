import { Inject, Injectable } from '@nestjs/common';
import { eq } from 'drizzle-orm';
import PDFDocument from 'pdfkit';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, cellarLocations } from '../db/schema.js';

const CSV_COLUMNS = [
  'name',
  'producer',
  'region',
  'appellation',
  'grapeVarieties',
  'vintage',
  'color',
  'quantity',
  'purchasePriceCents',
  'purchaseDate',
  'drinkFromYear',
  'drinkUntilYear',
  'location',
  'status',
  'notes',
] as const;

function csvEscape(value: unknown): string {
  if (value === null || value === undefined) return '';
  const str = Array.isArray(value) ? value.join('; ') : String(value);
  if (/[",\n]/.test(str)) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

@Injectable()
export class ExportService {
  constructor(@Inject(DRIZZLE) private readonly db: DrizzleDb) {}

  private async getCellarRows(householdId: string) {
    const rows = await this.db
      .select({
        bottle: bottles,
        locationLabel: cellarLocations.label,
      })
      .from(bottles)
      .leftJoin(cellarLocations, eq(cellarLocations.id, bottles.locationId))
      .where(eq(bottles.householdId, householdId));

    return rows.map(({ bottle, locationLabel }) => ({
      ...bottle,
      location: locationLabel,
    }));
  }

  async exportCsv(householdId: string): Promise<string> {
    const rows = await this.getCellarRows(householdId);
    const header = CSV_COLUMNS.join(',');
    const lines = rows.map((row) =>
      CSV_COLUMNS.map((col) => csvEscape((row as Record<string, unknown>)[col])).join(','),
    );
    return [header, ...lines].join('\n');
  }

  async exportPdf(householdId: string): Promise<Buffer> {
    const rows = await this.getCellarRows(householdId);

    return new Promise((resolvePromise, reject) => {
      const doc = new PDFDocument({ margin: 40, size: 'A4' });
      const chunks: Buffer[] = [];
      doc.on('data', (chunk) => chunks.push(chunk));
      doc.on('end', () => resolvePromise(Buffer.concat(chunks)));
      doc.on('error', reject);

      doc.fontSize(18).text('Ma cave à vin', { align: 'left' });
      doc
        .fontSize(10)
        .fillColor('gray')
        .text(`Export du ${new Date().toLocaleDateString('fr-FR')} -- ${rows.length} bouteille(s)`);
      doc.moveDown();

      for (const row of rows) {
        doc
          .fillColor('black')
          .fontSize(12)
          .text(
            `${row.name}${row.vintage ? ` (${row.vintage})` : ''} -- ${row.color}`,
            { continued: false },
          );
        const details = [
          row.producer,
          row.region,
          row.appellation,
          row.location ? `Emplacement : ${row.location}` : null,
          `Quantité : ${row.quantity}`,
          row.purchasePriceCents
            ? `Prix d'achat : ${(row.purchasePriceCents / 100).toFixed(2)} €`
            : null,
          row.drinkUntilYear ? `À boire avant : ${row.drinkUntilYear}` : null,
        ]
          .filter(Boolean)
          .join(' · ');
        if (details) {
          doc.fontSize(9).fillColor('gray').text(details);
        }
        doc.moveDown(0.5);
      }

      doc.end();
    });
  }
}
