CREATE TYPE "public"."ai_provider" AS ENUM('anthropic', 'openai', 'google');--> statement-breakpoint
CREATE TYPE "public"."ai_usage" AS ENUM('recognition', 'pairing', 'both');--> statement-breakpoint
CREATE TYPE "public"."bottle_status" AS ENUM('in_cellar', 'consumed');--> statement-breakpoint
CREATE TYPE "public"."user_role" AS ENUM('owner', 'member');--> statement-breakpoint
CREATE TYPE "public"."wine_color" AS ENUM('red', 'white', 'rose', 'sparkling', 'sweet', 'fortified');--> statement-breakpoint
CREATE TABLE "ai_provider_configs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"household_id" uuid NOT NULL,
	"provider" "ai_provider" NOT NULL,
	"api_key_encrypted" text NOT NULL,
	"usage" "ai_usage" DEFAULT 'both' NOT NULL,
	"is_default" boolean DEFAULT false NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "bottles" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"household_id" uuid NOT NULL,
	"name" text NOT NULL,
	"producer" text,
	"region" text,
	"appellation" text,
	"grape_varieties" text[],
	"vintage" integer,
	"color" "wine_color" NOT NULL,
	"quantity" integer DEFAULT 1 NOT NULL,
	"purchase_price_cents" integer,
	"purchase_date" date,
	"drink_from_year" integer,
	"drink_until_year" integer,
	"location_id" uuid,
	"label_photo_url" text,
	"status" "bottle_status" DEFAULT 'in_cellar' NOT NULL,
	"notes" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "cellar_locations" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"unit_id" uuid NOT NULL,
	"row" integer NOT NULL,
	"column" integer NOT NULL,
	"label" text NOT NULL
);
--> statement-breakpoint
CREATE TABLE "cellar_units" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"household_id" uuid NOT NULL,
	"name" text NOT NULL,
	"row_count" integer NOT NULL,
	"column_count" integer NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "households" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"name" text NOT NULL,
	"invite_code" text NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "households_invite_code_unique" UNIQUE("invite_code")
);
--> statement-breakpoint
CREATE TABLE "pairing_suggestions" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"household_id" uuid NOT NULL,
	"dish_description" text NOT NULL,
	"suggested_bottle_ids" uuid[] NOT NULL,
	"provider" "ai_provider" NOT NULL,
	"raw_response" text NOT NULL,
	"structured_fields" jsonb NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "scan_results" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"household_id" uuid NOT NULL,
	"bottle_id" uuid,
	"provider" "ai_provider" NOT NULL,
	"photo_url" text NOT NULL,
	"raw_response" text NOT NULL,
	"structured_fields" jsonb NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "tasting_notes" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"bottle_id" uuid NOT NULL,
	"user_id" uuid NOT NULL,
	"rating" integer,
	"comment" text,
	"consumed_date" date NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "users" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"email" text NOT NULL,
	"password_hash" text NOT NULL,
	"display_name" text NOT NULL,
	"household_id" uuid NOT NULL,
	"role" "user_role" DEFAULT 'member' NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "users_email_unique" UNIQUE("email")
);
--> statement-breakpoint
CREATE TABLE "wishlist_items" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"household_id" uuid NOT NULL,
	"name" text NOT NULL,
	"region" text,
	"notes" text,
	"target_price_cents" integer,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "ai_provider_configs" ADD CONSTRAINT "ai_provider_configs_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "bottles" ADD CONSTRAINT "bottles_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "bottles" ADD CONSTRAINT "bottles_location_id_cellar_locations_id_fk" FOREIGN KEY ("location_id") REFERENCES "public"."cellar_locations"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "cellar_locations" ADD CONSTRAINT "cellar_locations_unit_id_cellar_units_id_fk" FOREIGN KEY ("unit_id") REFERENCES "public"."cellar_units"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "cellar_units" ADD CONSTRAINT "cellar_units_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "pairing_suggestions" ADD CONSTRAINT "pairing_suggestions_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "scan_results" ADD CONSTRAINT "scan_results_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "scan_results" ADD CONSTRAINT "scan_results_bottle_id_bottles_id_fk" FOREIGN KEY ("bottle_id") REFERENCES "public"."bottles"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "tasting_notes" ADD CONSTRAINT "tasting_notes_bottle_id_bottles_id_fk" FOREIGN KEY ("bottle_id") REFERENCES "public"."bottles"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "tasting_notes" ADD CONSTRAINT "tasting_notes_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "users" ADD CONSTRAINT "users_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "wishlist_items" ADD CONSTRAINT "wishlist_items_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE UNIQUE INDEX "ai_provider_configs_household_provider_idx" ON "ai_provider_configs" USING btree ("household_id","provider");--> statement-breakpoint
CREATE UNIQUE INDEX "cellar_locations_unit_row_column_idx" ON "cellar_locations" USING btree ("unit_id","row","column");