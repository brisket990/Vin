import { drizzle } from 'drizzle-orm/node-postgres';
import { migrate } from 'drizzle-orm/node-postgres/migrator';
import { Pool } from 'pg';

/**
 * Standalone migration runner, executed as a separate step before the API
 * starts (see Dockerfile / docker-compose). Kept independent from Nest's DI
 * so it can run as a one-off command without booting the whole application.
 */
async function main() {
  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error('DATABASE_URL est requis pour appliquer les migrations.');
  }

  const pool = new Pool({ connectionString });
  const db = drizzle(pool);

  await migrate(db, { migrationsFolder: './src/db/migrations' });
  await pool.end();

  // eslint-disable-next-line no-console
  console.log('Migrations Drizzle appliquées avec succès.');
}

main().catch((error: unknown) => {
  // eslint-disable-next-line no-console
  console.error('Échec de la migration :', error);
  process.exit(1);
});
