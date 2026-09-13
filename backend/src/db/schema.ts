import {
  pgTable,
  pgEnum,
  uuid,
  text,
  integer,
  boolean,
  timestamp,
  date,
  jsonb,
  uniqueIndex,
} from 'drizzle-orm/pg-core';
import { relations } from 'drizzle-orm';

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

export const userRoleEnum = pgEnum('user_role', ['owner', 'member']);

export const aiProviderEnum = pgEnum('ai_provider', [
  'anthropic',
  'openai',
  'google',
  'mistral',
  'openrouter',
  'deepseek',
  'ollama',
]);

export const aiUsageEnum = pgEnum('ai_usage', [
  'recognition',
  'pairing',
  'both',
]);

export const wineColorEnum = pgEnum('wine_color', [
  'red',
  'white',
  'rose',
  'sparkling',
  'sweet',
  'fortified',
]);

// Wishlist (bottles not yet owned) is modeled by the separate wishlistItems
// table below, not as a bottle status -- a Bottle row always represents
// something the household actually owns (or owned, if consumed).
export const bottleStatusEnum = pgEnum('bottle_status', [
  'in_cellar',
  'consumed',
]);

// Re-exported as plain arrays so DTOs (class-validator @IsIn) and other
// non-Drizzle code can reference the same source of truth without importing
// pg-core enum internals.
export const userRoleValues = userRoleEnum.enumValues;
export const aiProviderValues = aiProviderEnum.enumValues;
export const aiUsageValues = aiUsageEnum.enumValues;
export const wineColorValues = wineColorEnum.enumValues;
export const bottleStatusValues = bottleStatusEnum.enumValues;

// ---------------------------------------------------------------------------
// Household / Users
// ---------------------------------------------------------------------------

export const households = pgTable('households', {
  id: uuid('id').defaultRandom().primaryKey(),
  name: text('name').notNull(),
  inviteCode: text('invite_code').notNull().unique(),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

export const users = pgTable('users', {
  id: uuid('id').defaultRandom().primaryKey(),
  email: text('email').notNull().unique(),
  passwordHash: text('password_hash').notNull(),
  displayName: text('display_name').notNull(),
  householdId: uuid('household_id')
    .notNull()
    .references(() => households.id, { onDelete: 'cascade' }),
  role: userRoleEnum('role').notNull().default('member'),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

// ---------------------------------------------------------------------------
// AI provider configuration (BYOK, per household)
// ---------------------------------------------------------------------------

export const aiProviderConfigs = pgTable(
  'ai_provider_configs',
  {
    id: uuid('id').defaultRandom().primaryKey(),
    householdId: uuid('household_id')
      .notNull()
      .references(() => households.id, { onDelete: 'cascade' }),
    provider: aiProviderEnum('provider').notNull(),
    // AES-256-GCM ciphertext (iv + authTag + ciphertext, base64) -- never store plaintext keys.
    // For 'ollama' (no key needed on a local network) this stores the
    // encrypted empty string rather than being null, so the column can stay
    // NOT NULL for every provider.
    apiKeyEncrypted: text('api_key_encrypted').notNull(),
    // Optional override of the default model for this provider (model names
    // and availability change frequently -- see ai-provider/provider-defaults.ts).
    model: text('model'),
    // Only used by 'ollama': the household's self-hosted server address
    // (e.g. http://192.168.1.50:11434), no path suffix.
    baseUrl: text('base_url'),
    usage: aiUsageEnum('usage').notNull().default('both'),
    isDefault: boolean('is_default').notNull().default(false),
    createdAt: timestamp('created_at', { withTimezone: true })
      .defaultNow()
      .notNull(),
    updatedAt: timestamp('updated_at', { withTimezone: true })
      .defaultNow()
      .notNull(),
  },
  (table) => [
    uniqueIndex('ai_provider_configs_household_provider_idx').on(
      table.householdId,
      table.provider,
    ),
  ],
);

// ---------------------------------------------------------------------------
// Cellar structure (grid of numbered slots)
// ---------------------------------------------------------------------------

export const cellarUnits = pgTable('cellar_units', {
  id: uuid('id').defaultRandom().primaryKey(),
  householdId: uuid('household_id')
    .notNull()
    .references(() => households.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  rowCount: integer('row_count').notNull(),
  columnCount: integer('column_count').notNull(),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

export const cellarLocations = pgTable(
  'cellar_locations',
  {
    id: uuid('id').defaultRandom().primaryKey(),
    unitId: uuid('unit_id')
      .notNull()
      .references(() => cellarUnits.id, { onDelete: 'cascade' }),
    row: integer('row').notNull(),
    column: integer('column').notNull(),
    label: text('label').notNull(),
  },
  (table) => [
    uniqueIndex('cellar_locations_unit_row_column_idx').on(
      table.unitId,
      table.row,
      table.column,
    ),
  ],
);

// ---------------------------------------------------------------------------
// Bottles
// ---------------------------------------------------------------------------

export const bottles = pgTable('bottles', {
  id: uuid('id').defaultRandom().primaryKey(),
  householdId: uuid('household_id')
    .notNull()
    .references(() => households.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  producer: text('producer'),
  region: text('region'),
  appellation: text('appellation'),
  grapeVarieties: text('grape_varieties').array(),
  vintage: integer('vintage'),
  color: wineColorEnum('color').notNull(),
  quantity: integer('quantity').notNull().default(1),
  purchasePriceCents: integer('purchase_price_cents'),
  purchaseDate: date('purchase_date'),
  drinkFromYear: integer('drink_from_year'),
  drinkUntilYear: integer('drink_until_year'),
  locationId: uuid('location_id').references(() => cellarLocations.id, {
    onDelete: 'set null',
  }),
  labelPhotoUrl: text('label_photo_url'),
  status: bottleStatusEnum('status').notNull().default('in_cellar'),
  notes: text('notes'),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
  updatedAt: timestamp('updated_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

// ---------------------------------------------------------------------------
// AI scan results (label recognition) -- raw + structured kept together
// ---------------------------------------------------------------------------

export const scanResults = pgTable('scan_results', {
  id: uuid('id').defaultRandom().primaryKey(),
  householdId: uuid('household_id')
    .notNull()
    .references(() => households.id, { onDelete: 'cascade' }),
  bottleId: uuid('bottle_id').references(() => bottles.id, {
    onDelete: 'set null',
  }),
  provider: aiProviderEnum('provider').notNull(),
  photoUrl: text('photo_url').notNull(),
  rawResponse: text('raw_response').notNull(),
  structuredFields: jsonb('structured_fields').notNull(),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

// ---------------------------------------------------------------------------
// Tasting notes (carnet de dégustation)
// ---------------------------------------------------------------------------

export const tastingNotes = pgTable('tasting_notes', {
  id: uuid('id').defaultRandom().primaryKey(),
  bottleId: uuid('bottle_id')
    .notNull()
    .references(() => bottles.id, { onDelete: 'cascade' }),
  userId: uuid('user_id')
    .notNull()
    .references(() => users.id, { onDelete: 'cascade' }),
  rating: integer('rating'),
  comment: text('comment'),
  consumedDate: date('consumed_date').notNull(),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

// ---------------------------------------------------------------------------
// Food pairing suggestions -- raw + structured kept together
// ---------------------------------------------------------------------------

export const pairingSuggestions = pgTable('pairing_suggestions', {
  id: uuid('id').defaultRandom().primaryKey(),
  householdId: uuid('household_id')
    .notNull()
    .references(() => households.id, { onDelete: 'cascade' }),
  dishDescription: text('dish_description').notNull(),
  suggestedBottleIds: uuid('suggested_bottle_ids').array().notNull(),
  provider: aiProviderEnum('provider').notNull(),
  rawResponse: text('raw_response').notNull(),
  structuredFields: jsonb('structured_fields').notNull(),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

// ---------------------------------------------------------------------------
// Wishlist
// ---------------------------------------------------------------------------

export const wishlistItems = pgTable('wishlist_items', {
  id: uuid('id').defaultRandom().primaryKey(),
  householdId: uuid('household_id')
    .notNull()
    .references(() => households.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  region: text('region'),
  notes: text('notes'),
  targetPriceCents: integer('target_price_cents'),
  createdAt: timestamp('created_at', { withTimezone: true })
    .defaultNow()
    .notNull(),
});

// ---------------------------------------------------------------------------
// Relations (for query convenience via drizzle's relational query builder)
// ---------------------------------------------------------------------------

export const householdsRelations = relations(households, ({ many }) => ({
  users: many(users),
  aiProviderConfigs: many(aiProviderConfigs),
  cellarUnits: many(cellarUnits),
  bottles: many(bottles),
  pairingSuggestions: many(pairingSuggestions),
  wishlistItems: many(wishlistItems),
}));

export const usersRelations = relations(users, ({ one, many }) => ({
  household: one(households, {
    fields: [users.householdId],
    references: [households.id],
  }),
  tastingNotes: many(tastingNotes),
}));

export const cellarUnitsRelations = relations(
  cellarUnits,
  ({ one, many }) => ({
    household: one(households, {
      fields: [cellarUnits.householdId],
      references: [households.id],
    }),
    locations: many(cellarLocations),
  }),
);

export const cellarLocationsRelations = relations(
  cellarLocations,
  ({ one, many }) => ({
    unit: one(cellarUnits, {
      fields: [cellarLocations.unitId],
      references: [cellarUnits.id],
    }),
    bottles: many(bottles),
  }),
);

export const bottlesRelations = relations(bottles, ({ one, many }) => ({
  household: one(households, {
    fields: [bottles.householdId],
    references: [households.id],
  }),
  location: one(cellarLocations, {
    fields: [bottles.locationId],
    references: [cellarLocations.id],
  }),
  scanResults: many(scanResults),
  tastingNotes: many(tastingNotes),
}));

export const scanResultsRelations = relations(scanResults, ({ one }) => ({
  household: one(households, {
    fields: [scanResults.householdId],
    references: [households.id],
  }),
  bottle: one(bottles, {
    fields: [scanResults.bottleId],
    references: [bottles.id],
  }),
}));

export const tastingNotesRelations = relations(tastingNotes, ({ one }) => ({
  bottle: one(bottles, {
    fields: [tastingNotes.bottleId],
    references: [bottles.id],
  }),
  user: one(users, {
    fields: [tastingNotes.userId],
    references: [users.id],
  }),
}));

export const pairingSuggestionsRelations = relations(
  pairingSuggestions,
  ({ one }) => ({
    household: one(households, {
      fields: [pairingSuggestions.householdId],
      references: [households.id],
    }),
  }),
);

export const wishlistItemsRelations = relations(
  wishlistItems,
  ({ one }) => ({
    household: one(households, {
      fields: [wishlistItems.householdId],
      references: [households.id],
    }),
  }),
);
