import { Inject, Injectable, NotFoundException } from '@nestjs/common';
import { and, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { wishlistItems } from '../db/schema.js';
import { BottleService } from '../bottle/bottle.service.js';
import type { CreateWishlistItemDto } from './dto/create-wishlist-item.dto.js';
import type { UpdateWishlistItemDto } from './dto/update-wishlist-item.dto.js';
import type { ConvertWishlistItemDto } from './dto/convert-wishlist-item.dto.js';

@Injectable()
export class WishlistService {
  constructor(
    @Inject(DRIZZLE) private readonly db: DrizzleDb,
    private readonly bottleService: BottleService,
  ) {}

  async create(householdId: string, dto: CreateWishlistItemDto) {
    const [item] = await this.db
      .insert(wishlistItems)
      .values({ householdId, ...dto })
      .returning();
    return item;
  }

  async findAll(householdId: string) {
    return this.db
      .select()
      .from(wishlistItems)
      .where(eq(wishlistItems.householdId, householdId));
  }

  private async getOwnedOrThrow(householdId: string, id: string) {
    const [item] = await this.db
      .select()
      .from(wishlistItems)
      .where(and(eq(wishlistItems.id, id), eq(wishlistItems.householdId, householdId)))
      .limit(1);

    if (!item) throw new NotFoundException("Bouteille de la liste d'envies introuvable.");
    return item;
  }

  async update(householdId: string, id: string, dto: UpdateWishlistItemDto) {
    await this.getOwnedOrThrow(householdId, id);
    const [updated] = await this.db
      .update(wishlistItems)
      .set(dto)
      .where(eq(wishlistItems.id, id))
      .returning();
    return updated;
  }

  async remove(householdId: string, id: string) {
    await this.getOwnedOrThrow(householdId, id);
    await this.db.delete(wishlistItems).where(eq(wishlistItems.id, id));
    return { deleted: true };
  }

  /** "Je viens de l'acheter" -- turns a wishlist entry into a real cellar bottle. */
  async convertToBottle(
    householdId: string,
    id: string,
    dto: ConvertWishlistItemDto,
  ) {
    const item = await this.getOwnedOrThrow(householdId, id);

    const bottle = await this.bottleService.create(householdId, {
      name: item.name,
      region: item.region ?? undefined,
      color: dto.color,
      locationId: dto.locationId,
      purchasePriceCents: item.targetPriceCents ?? undefined,
      notes: item.notes ?? undefined,
    });

    await this.db.delete(wishlistItems).where(eq(wishlistItems.id, id));
    return bottle;
  }
}
