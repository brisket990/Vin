import { Inject, Injectable, NotFoundException } from '@nestjs/common';
import { and, desc, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { bottles, tastingNotes } from '../db/schema.js';
import type { UpdateTastingNoteDto } from './dto/update-tasting-note.dto.js';

@Injectable()
export class TastingService {
  constructor(@Inject(DRIZZLE) private readonly db: DrizzleDb) {}

  async findAll(householdId: string) {
    return this.db
      .select({
        id: tastingNotes.id,
        bottleId: tastingNotes.bottleId,
        bottleName: bottles.name,
        bottleVintage: bottles.vintage,
        userId: tastingNotes.userId,
        rating: tastingNotes.rating,
        comment: tastingNotes.comment,
        consumedDate: tastingNotes.consumedDate,
        createdAt: tastingNotes.createdAt,
      })
      .from(tastingNotes)
      .innerJoin(bottles, eq(bottles.id, tastingNotes.bottleId))
      .where(eq(bottles.householdId, householdId))
      .orderBy(desc(tastingNotes.consumedDate));
  }

  private async getOwnedOrThrow(householdId: string, id: string) {
    const [note] = await this.db
      .select({ note: tastingNotes, householdId: bottles.householdId })
      .from(tastingNotes)
      .innerJoin(bottles, eq(bottles.id, tastingNotes.bottleId))
      .where(and(eq(tastingNotes.id, id), eq(bottles.householdId, householdId)))
      .limit(1);

    if (!note) throw new NotFoundException('Note de dégustation introuvable.');
    return note.note;
  }

  async update(householdId: string, id: string, dto: UpdateTastingNoteDto) {
    await this.getOwnedOrThrow(householdId, id);
    const [updated] = await this.db
      .update(tastingNotes)
      .set(dto)
      .where(eq(tastingNotes.id, id))
      .returning();
    return updated;
  }

  async remove(householdId: string, id: string) {
    await this.getOwnedOrThrow(householdId, id);
    await this.db.delete(tastingNotes).where(eq(tastingNotes.id, id));
    return { deleted: true };
  }
}
