CREATE TABLE "cellar_sites" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"household_id" uuid NOT NULL,
	"name" text NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "cellar_sites" ADD CONSTRAINT "cellar_sites_household_id_households_id_fk" FOREIGN KEY ("household_id") REFERENCES "public"."households"("id") ON DELETE cascade ON UPDATE no action;
--> statement-breakpoint
-- Add the column nullable first so existing rows (any household that
-- already has cellar units) don't violate NOT NULL before they're backfilled.
ALTER TABLE "cellar_units" ADD COLUMN "site_id" uuid;
--> statement-breakpoint
-- One default site per household that already has at least one unit, so
-- pre-existing casiers land somewhere sensible instead of being orphaned.
INSERT INTO "cellar_sites" ("household_id", "name")
SELECT DISTINCT "household_id", 'Cave principale'
FROM "cellar_units"
WHERE "site_id" IS NULL;
--> statement-breakpoint
UPDATE "cellar_units" cu
SET "site_id" = cs."id"
FROM "cellar_sites" cs
WHERE cu."site_id" IS NULL AND cs."household_id" = cu."household_id" AND cs."name" = 'Cave principale';
--> statement-breakpoint
ALTER TABLE "cellar_units" ALTER COLUMN "site_id" SET NOT NULL;
--> statement-breakpoint
ALTER TABLE "cellar_units" ADD CONSTRAINT "cellar_units_site_id_cellar_sites_id_fk" FOREIGN KEY ("site_id") REFERENCES "public"."cellar_sites"("id") ON DELETE cascade ON UPDATE no action;
