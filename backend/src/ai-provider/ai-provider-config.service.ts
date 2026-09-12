import {
  BadRequestException,
  Inject,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { and, eq } from 'drizzle-orm';
import { DRIZZLE, type DrizzleDb } from '../db/drizzle.module.js';
import { aiProviderConfigs, type aiUsageValues } from '../db/schema.js';
import {
  decryptSecret,
  encryptSecret,
  maskSecret,
} from '../common/crypto/secret-cipher.js';
import { DEFAULT_MODELS } from './provider-defaults.js';
import type { UpsertAiProviderConfigDto } from './dto/upsert-ai-provider-config.dto.js';

@Injectable()
export class AiProviderConfigService {
  constructor(
    @Inject(DRIZZLE) private readonly db: DrizzleDb,
    private readonly config: ConfigService,
  ) {}

  private get encryptionSecret(): string {
    return this.config.getOrThrow<string>('API_KEY_ENCRYPTION_SECRET');
  }

  async list(householdId: string) {
    const configs = await this.db
      .select()
      .from(aiProviderConfigs)
      .where(eq(aiProviderConfigs.householdId, householdId));

    return configs.map((c) => {
      let keyHint = '••••';
      try {
        keyHint = maskSecret(decryptSecret(c.apiKeyEncrypted, this.encryptionSecret));
      } catch {
        // Leave the generic placeholder if decryption fails for any reason.
      }
      return {
        id: c.id,
        provider: c.provider,
        model: c.model ?? DEFAULT_MODELS[c.provider],
        usage: c.usage,
        isDefault: c.isDefault,
        keyHint,
        createdAt: c.createdAt,
        updatedAt: c.updatedAt,
      };
    });
  }

  /** Creates or replaces the household's config for a given provider (one config per provider per household). */
  async upsert(householdId: string, dto: UpsertAiProviderConfigDto) {
    const apiKeyEncrypted = encryptSecret(dto.apiKey, this.encryptionSecret);

    const [config] = await this.db
      .insert(aiProviderConfigs)
      .values({
        householdId,
        provider: dto.provider,
        apiKeyEncrypted,
        model: dto.model,
        usage: dto.usage ?? 'both',
        isDefault: dto.isDefault ?? false,
      })
      .onConflictDoUpdate({
        target: [aiProviderConfigs.householdId, aiProviderConfigs.provider],
        set: {
          apiKeyEncrypted,
          model: dto.model,
          usage: dto.usage ?? 'both',
          isDefault: dto.isDefault ?? false,
          updatedAt: new Date(),
        },
      })
      .returning();

    return {
      id: config.id,
      provider: config.provider,
      model: config.model ?? DEFAULT_MODELS[config.provider],
      usage: config.usage,
      isDefault: config.isDefault,
      keyHint: maskSecret(dto.apiKey),
    };
  }

  async remove(householdId: string, id: string) {
    const [deleted] = await this.db
      .delete(aiProviderConfigs)
      .where(
        and(eq(aiProviderConfigs.id, id), eq(aiProviderConfigs.householdId, householdId)),
      )
      .returning();

    if (!deleted) throw new NotFoundException("Configuration IA introuvable.");
    return { deleted: true };
  }

  /**
   * Resolves the decrypted credentials to use for a given usage
   * (recognition/pairing). Prefers a config explicitly marked isDefault,
   * then falls back to the first configured provider covering that usage.
   * Pass `preferredProvider` to let the app pick a specific one per call.
   */
  async resolveForUsage(
    householdId: string,
    usage: (typeof aiUsageValues)[number],
    preferredProvider?: string,
  ) {
    const configs = await this.db
      .select()
      .from(aiProviderConfigs)
      .where(eq(aiProviderConfigs.householdId, householdId));

    const eligible = configs.filter(
      (c) => c.usage === usage || c.usage === 'both',
    );

    if (eligible.length === 0) {
      throw new BadRequestException(
        "Aucun fournisseur IA configuré pour cet usage -- ajoute une clé API dans les réglages.",
      );
    }

    const chosen =
      (preferredProvider &&
        eligible.find((c) => c.provider === preferredProvider)) ||
      eligible.find((c) => c.isDefault) ||
      eligible[0];

    return {
      provider: chosen.provider,
      apiKey: decryptSecret(chosen.apiKeyEncrypted, this.encryptionSecret),
      model: chosen.model ?? DEFAULT_MODELS[chosen.provider],
    };
  }
}
