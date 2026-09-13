ALTER TYPE "public"."ai_provider" ADD VALUE 'mistral';--> statement-breakpoint
ALTER TYPE "public"."ai_provider" ADD VALUE 'openrouter';--> statement-breakpoint
ALTER TYPE "public"."ai_provider" ADD VALUE 'deepseek';--> statement-breakpoint
ALTER TYPE "public"."ai_provider" ADD VALUE 'ollama';--> statement-breakpoint
ALTER TABLE "ai_provider_configs" ADD COLUMN "base_url" text;