# Vin — API backend

API REST (NestJS + Drizzle ORM + PostgreSQL) pour l'application de gestion de cave à vin. Voir `/projects` (doc "spec-technique.md") pour la spécification complète.

## Stack

- NestJS 12 (ESM, Node 22+)
- Drizzle ORM + PostgreSQL (choisi plutôt que Prisma : pas de binaire natif à télécharger, ce qui posait problème dans l'environnement de build utilisé pour ce projet)
- JWT (via `@nestjs/jwt` / `passport-jwt`)
- IA "bring your own key" : Anthropic, OpenAI, Google Gemini (appelés en HTTP direct, pas de SDK)
- Export CSV (fait main) + PDF (`pdfkit`)

## Démarrer en local

```bash
cp .env.example .env   # puis ajuster les secrets
docker compose up -d db   # lance juste Postgres
npm install
npm run db:generate    # si tu modifies src/db/schema.ts
npm run build
npm run db:migrate     # applique les migrations
npm run start:dev
```

L'API écoute sur `http://localhost:3000/api/vin` (préfixe global `api/vin`). `GET /api/vin/health` ne nécessite pas d'authentification.

## Tests

```bash
DATABASE_URL=postgresql://vin:vin@localhost:5432/vin npm test
```

Les tests touchant la base sont des tests d'intégration légers (pas de mock de Drizzle) : ils ont besoin d'un Postgres local avec les migrations appliquées. Les appels aux fournisseurs IA sont mockés (`fetch` stubé) dans leurs tests.

## Aperçu des routes

| Domaine | Routes principales |
|---|---|
| Auth | `POST /auth/register-household`, `POST /auth/join-household`, `POST /auth/login`, `GET /auth/me` |
| Foyer | `GET /household/me`, `PATCH /household/me`, `POST /household/me/regenerate-invite-code` |
| Cave | `GET/POST /cellar/units`, `GET /cellar/units/:id`, `POST /cellar/units/:id/suggest-location` |
| Bouteilles | `POST/GET /bottles`, `GET/PATCH/DELETE /bottles/:id`, `POST /bottles/:id/consume` |
| Fournisseurs IA | `GET/POST /ai-providers`, `DELETE /ai-providers/:id` |
| Scan étiquette | `POST /scan` (multipart, champ `photo`), `GET /scan`, `GET /scan/:id`, `PATCH /scan/:id/link-bottle` |
| Accords mets-vin | `POST /pairing`, `GET /pairing`, `GET /pairing/:id` |
| Dégustation | `GET /tasting-notes`, `PATCH/DELETE /tasting-notes/:id` |
| Liste d'envies | `POST/GET /wishlist`, `PATCH/DELETE /wishlist/:id`, `POST /wishlist/:id/convert-to-bottle` |
| Tableau de bord | `GET /dashboard` |
| Export | `GET /export/cellar.csv`, `GET /export/cellar.pdf` |

Toutes les routes sauf `/health`, `/auth/register-household`, `/auth/join-household` et `/auth/login` exigent un header `Authorization: Bearer <token>`.

## Déploiement

Voir `Dockerfile` (build multi-stage + migration au démarrage) et `docker-compose.yml`. Notes de déploiement Coolify détaillées à venir (voir la tâche dédiée dans le suivi de projet).
