import { checkedSample, type Sample, sampleKey } from "./projection.ts";

export type Vote = "funcionó" | "falló" | "corregido";
export type Event = { id: string; actionId: string; status: Vote } & Record<string, unknown>;
export type Finish = "proposed" | "no_proposal" | "rejected" | "already_fixed" | "temporary_error";

export interface FeedbackQueue {
  issue(installation: string, digest: string): boolean;
  revoke(installation: string): void;
  authorize(digest: string): string | null;
  record(installation: string, event: Event, sample: Sample | null, now: number): void;
  lease(now: number): { lease: string; cases: { key: string; sample: Sample; installations: number }[] };
  finish(key: string, lease: string, status: Finish, now: number): boolean;
  recent(): unknown[];
}

/** Feedback uses its own tables in the same private Durable Object and no MCP routes. */
export class SqlFeedbackQueue implements FeedbackQueue {
  private readonly sql: SqlStorage;

  constructor(sql: SqlStorage) {
    this.sql = sql;
    sql.exec(`CREATE TABLE IF NOT EXISTS feedback_credentials(
      installation TEXT PRIMARY KEY, digest TEXT UNIQUE, revoked INTEGER NOT NULL DEFAULT 0)`);
    sql.exec(`CREATE TABLE IF NOT EXISTS feedback_cases(
      key TEXT PRIMARY KEY, sample TEXT NOT NULL, lease TEXT, until INTEGER NOT NULL DEFAULT 0,
      state TEXT NOT NULL DEFAULT 'pending', reason TEXT)`);
    sql.exec(`CREATE TABLE IF NOT EXISTS feedback_events(
      id TEXT PRIMARY KEY, action_id TEXT NOT NULL, installation TEXT NOT NULL, status TEXT NOT NULL,
      case_key TEXT, payload TEXT NOT NULL, received INTEGER NOT NULL)`);
    sql.exec("CREATE INDEX IF NOT EXISTS feedback_events_case ON feedback_events(case_key, installation)");
    sql.exec("CREATE INDEX IF NOT EXISTS feedback_events_age ON feedback_events(received)");
    sql.exec("CREATE INDEX IF NOT EXISTS feedback_events_action ON feedback_events(installation, action_id)");
  }

  issue(installation: string, digest: string) {
    return this.sql.exec(`INSERT INTO feedback_credentials(installation, digest) VALUES(?, ?)
      ON CONFLICT(installation) DO UPDATE SET digest = excluded.digest WHERE revoked = 0`, installation, digest).rowsWritten > 0;
  }

  revoke(installation: string) {
    this.sql.exec(`INSERT INTO feedback_credentials(installation, revoked) VALUES(?, 1)
      ON CONFLICT(installation) DO UPDATE SET revoked = 1, digest = NULL`, installation);
  }

  authorize(digest: string) {
    const result = this.sql.exec("SELECT installation FROM feedback_credentials WHERE digest = ? AND revoked = 0", digest).toArray()[0];
    return result ? String(result.installation) : null;
  }

  record(installation: string, event: Event, sample: Sample | null, now: number) {
    const key = sample ? sampleKey(sample) : null;
    if (sample) this.sql.exec("INSERT OR IGNORE INTO feedback_cases(key, sample) VALUES(?, ?)", key, JSON.stringify(sample));
    this.sql.exec(`INSERT OR IGNORE INTO feedback_events(id, action_id, installation, status, case_key, payload, received)
      VALUES(?, ?, ?, ?, ?, ?, ?)`, event.id, event.actionId, installation, event.status, key, JSON.stringify(event), now);
  }

  lease(now: number) {
    const id = crypto.randomUUID();
    this.sql.exec("DELETE FROM feedback_events WHERE received < ?", now - 30 * 86_400_000);
    const rows = this.sql.exec(`SELECT c.key, c.sample, COUNT(DISTINCT e.installation) AS installations
      FROM feedback_cases c JOIN feedback_events e ON e.case_key = c.key
      WHERE c.state = 'pending' AND c.until <= ? AND NOT EXISTS (
        SELECT 1 FROM feedback_events latest WHERE latest.installation = e.installation AND latest.action_id = e.action_id
        AND (latest.received > e.received OR (latest.received = e.received AND latest.rowid > e.rowid))
      )
      GROUP BY c.key HAVING MAX(CASE WHEN e.status = 'corregido' THEN 1 ELSE 0 END) = 1
      ORDER BY installations DESC, MAX(e.received) DESC LIMIT 20`, now).toArray();
    const cases = rows.flatMap((row) => {
      const sample = checkedSample(JSON.parse(String(row.sample)));
      if (!sample) return [];
      const key = String(row.key);
      this.sql.exec("UPDATE feedback_cases SET lease = ?, until = ? WHERE key = ?", id, now + 3_600_000, key);
      return [{ key, sample, installations: Number(row.installations) }];
    });
    return { lease: id, cases };
  }

  finish(key: string, lease: string, status: Finish, now: number) {
    const closed = status === "proposed" || status === "already_fixed";
    return this.sql.exec(`UPDATE feedback_cases SET state = ?, reason = ?, lease = NULL, until = ?
      WHERE key = ? AND lease = ? AND until > ?`,
      closed ? "reviewed" : "pending", status, closed ? 0 : now + 86_400_000, key, lease, now).rowsWritten > 0;
  }

  recent() {
    return this.sql.exec(`SELECT e.installation, e.payload, c.reason, c.state FROM feedback_events e
      LEFT JOIN feedback_cases c ON c.key = e.case_key ORDER BY e.received DESC LIMIT 20`).toArray()
      .map((r) => ({ installation: r.installation, event: JSON.parse(String(r.payload)), reason: r.reason, state: r.state }));
  }
}
