import Database from "better-sqlite3";
import type { IPassportStorage, PassportRecord } from "../core/interfaces/i_passport_storage.js";

export class SqlitePassportStorage implements IPassportStorage {
  private db: Database.Database;

  constructor(dbPath: string = ":memory:") {
    this.db = new Database(dbPath);
    this.init();
  }

  private init(): void {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS passports (
        public_id TEXT PRIMARY KEY,
        version INTEGER NOT NULL,
        blob_base64 TEXT NOT NULL,
        updated_at INTEGER NOT NULL
      );
    `);
  }

  public async savePassport(record: PassportRecord): Promise<boolean> {
    const existing = this.db
      .prepare("SELECT version FROM passports WHERE public_id = ?")
      .get(record.publicId) as { version: number } | undefined;

    if (existing && existing.version >= record.version) {
      // Monotonic guard: refuse stale or downgrade version
      return false;
    }

    this.db
      .prepare(
        `INSERT INTO passports (public_id, version, blob_base64, updated_at)
         VALUES (?, ?, ?, ?)
         ON CONFLICT(public_id) DO UPDATE SET
           version = excluded.version,
           blob_base64 = excluded.blob_base64,
           updated_at = excluded.updated_at`
      )
      .run(record.publicId, record.version, record.blobBase64, record.updatedAt);

    return true;
  }

  public async getPassport(publicId: string): Promise<PassportRecord | null> {
    const row = this.db
      .prepare("SELECT public_id, version, blob_base64, updated_at FROM passports WHERE public_id = ?")
      .get(publicId) as { public_id: string; version: number; blob_base64: string; updated_at: number } | undefined;

    if (!row) return null;

    return {
      publicId: row.public_id,
      version: row.version,
      blobBase64: row.blob_base64,
      updatedAt: row.updated_at,
    };
  }
}
