-- DROP TABLE ... CASCADE below already removes cellar_units' FK to
-- cellar_sites, so there's no separate constraint left to drop by name.
ALTER TABLE "cellar_sites" DISABLE ROW LEVEL SECURITY;--> statement-breakpoint
DROP TABLE "cellar_sites" CASCADE;--> statement-breakpoint
ALTER TABLE "cellar_units" DROP COLUMN "site_id";