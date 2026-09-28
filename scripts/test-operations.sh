#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

node --check cloudflare/src/index.js
node --check cloudflare/src/admin-page.js

python3 - <<'PY'
import sqlite3
from pathlib import Path

schema = Path("cloudflare/migrations/0002_game_operations.sql").read_text()
db = sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE leaderboard (
  player_id TEXT NOT NULL,
  game_id TEXT NOT NULL,
  mode_id TEXT NOT NULL,
  nickname TEXT NOT NULL,
  country_code TEXT NOT NULL,
  best_score INTEGER NOT NULL,
  achieved_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  PRIMARY KEY(player_id, game_id, mode_id)
);
""")
db.executescript(schema)
db.executescript(schema)

assert db.execute("SELECT COUNT(*) FROM game_catalog").fetchone()[0] == 12
assert db.execute("SELECT COUNT(*) FROM app_games WHERE app_id='yamone_arcade2' AND enabled=1").fetchone()[0] == 6
assert db.execute("SELECT ranking_epoch FROM game_catalog WHERE game_id='orbit_snap' AND mode_id='normal'").fetchone()[0] == 1
db.execute("UPDATE game_catalog SET ranking_epoch=ranking_epoch+1, local_reset_epoch=local_reset_epoch+1 WHERE game_id='orbit_snap' AND mode_id='normal'")
assert db.execute("SELECT ranking_epoch, local_reset_epoch FROM game_catalog WHERE game_id='orbit_snap' AND mode_id='normal'").fetchone() == (2, 1)
print("PASS operations migration is repeatable and seeds 12 shared modes / 6 Arcade 2 games")
PY

node --input-type=module <<'JS'
import { normalizeCountry } from './cloudflare/src/index.js';
import { ADMIN_HTML } from './cloudflare/src/admin-page.js';
if (normalizeCountry('kr') !== 'KR' || normalizeCountry('KOR') !== '') throw new Error('country normalization');
for (const marker of ['전체 통계', '국가별 통계', '사용자별 게임 기록', '게임 노출·순서·랭킹 관리', '랭킹+앱기록']) {
  if (!ADMIN_HTML.includes(marker)) throw new Error('admin marker: ' + marker);
}
const inlineScript = ADMIN_HTML.match(/<script>([\s\S]*?)<\/script>/)?.[1];
if (!inlineScript) throw new Error('admin inline script missing');
new Function(inlineScript);
console.log('PASS Worker validation helper and admin dashboard contract');
JS

echo "All operations checks passed."
