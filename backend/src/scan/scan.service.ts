import {
  BadRequestException,
  Inject,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { and, desc, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { scanResults } from '../db/schema.js';
import { StorageService } from '../storage/storage.service.js';
import { AiProviderService } from '../ai-provider/ai-provider.service.js';
import type { ScanPhotoDto } from './dto/scan-photo.dto.js';

const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

@Injectable()
export class ScanService {
  constructor(
    @Inject(DRIZZLE) private readonly db: DrizzleDb,
    private readonly storageService: StorageService,
    private readonly aiProviderService: AiProviderService,
  ) {}

  async scan(
    householdId: string,
    file: Express.Multer.File | undefined,
    dto: ScanPhotoDto,
  ) {
    if (!file) {
      throw new BadRequestException("Aucune photo reçue (champ 'photo' attendu).");
    }
    if (!ALLOWED_MIME_TYPES.has(file.mimetype)) {
      throw new BadRequestException(
        'Format de photo non supporté (jpeg, png ou webp attendu).',
      );
    }

    const { url: photoUrl } = await this.storageService.savePhoto(
      file.buffer,
      file.mimetype,
    );

    const { provider, client } = await this.aiProviderService.getClientForUsage(
      householdId,
      'recognition',
      dto.provider,
    );

    const { structured, rawResponse } = await client.recognizeLabel(
      file.buffer.toString('base64'),
      file.mimetype,
    );

    const [result] = await this.db
      .insert(scanResults)
      .values({
        householdId,
        provider,
        photoUrl,
        rawResponse,
        structuredFields: structured,
      })
      .returning();

    return result;
  }

  async findAll(householdId: string) {
    return this.db
      .select()
      .from(scanResults)
      .where(eq(scanResults.householdId, householdId))
      .orderBy(desc(scanResults.createdAt));
  }

  async findOne(householdId: string, id: string) {
    const [result] = await this.db
      .select()
      .from(scanResults)
      .where(and(eq(scanResults.id, id), eq(scanResults.householdId, householdId)))
      .limit(1);

    if (!result) throw new NotFoundException('Résultat de scan introuvable.');
    return result;
  }

  async linkBottle(householdId: string, id: string, bottleId: string) {
    await this.findOne(householdId, id);
    const [updated] = await this.db
      .update(scanResults)
      .set({ bottleId })
      .where(and(eq(scanResults.id, id), eq(scanResults.householdId, householdId)))
      .returning();
    return updated;
  }
}
