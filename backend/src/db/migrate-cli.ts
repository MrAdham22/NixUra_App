import { fileURLToPath } from 'node:url';
import { PgDb } from './pgDb.js';
import { migrate } from './migrate.js';

const url = process.env.DATABASE_URL;
if (!url) {
  console.error('DATABASE_URL is required');
  process.exit(1);
}
const db = new PgDb(url);
const dir = fileURLToPath(new URL('../../migrations', import.meta.url));
migrate(db, dir)
  .then((ran) => console.log(ran.length ? `Applied: ${ran.join(', ')}` : 'Database is up to date'))
  .catch((e) => {
    console.error('Migration failed:', e instanceof Error ? e.message : e);
    process.exitCode = 1;
  })
  .finally(() => db.close());
