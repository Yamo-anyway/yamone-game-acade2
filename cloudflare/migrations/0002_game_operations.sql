-- Yamone shared ranking operations schema.
-- This migration only adds tables; the existing leaderboard table and rows stay intact.

CREATE TABLE IF NOT EXISTS game_catalog (
  game_id TEXT NOT NULL,
  mode_id TEXT NOT NULL,
  title TEXT NOT NULL,
  score_unit TEXT NOT NULL,
  max_score INTEGER NOT NULL CHECK (max_score > 0),
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'hidden', 'retired')),
  ranking_epoch INTEGER NOT NULL DEFAULT 1 CHECK (ranking_epoch > 0),
  local_reset_epoch INTEGER NOT NULL DEFAULT 0 CHECK (local_reset_epoch >= 0),
  created_at INTEGER NOT NULL DEFAULT (unixepoch()),
  updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
  PRIMARY KEY (game_id, mode_id)
);

CREATE TABLE IF NOT EXISTS app_games (
  app_id TEXT NOT NULL,
  game_id TEXT NOT NULL,
  mode_id TEXT NOT NULL,
  enabled INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)),
  display_order INTEGER NOT NULL DEFAULT 0,
  featured INTEGER NOT NULL DEFAULT 0 CHECK (featured IN (0, 1)),
  updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
  PRIMARY KEY (app_id, game_id, mode_id),
  FOREIGN KEY (game_id, mode_id) REFERENCES game_catalog(game_id, mode_id)
);

CREATE TABLE IF NOT EXISTS app_catalog_settings (
  app_id TEXT PRIMARY KEY,
  sort_mode TEXT NOT NULL DEFAULT 'manual'
    CHECK (sort_mode IN ('manual', 'popular_7d', 'popular_all')),
  updated_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE TABLE IF NOT EXISTS play_receipts (
  app_id TEXT NOT NULL,
  receipt_id TEXT NOT NULL,
  player_id TEXT NOT NULL,
  game_id TEXT NOT NULL,
  mode_id TEXT NOT NULL,
  country_code TEXT NOT NULL DEFAULT '',
  ranking_epoch INTEGER NOT NULL DEFAULT 1,
  score INTEGER,
  started_at INTEGER NOT NULL DEFAULT (unixepoch()),
  finished_at INTEGER,
  updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
  PRIMARY KEY (app_id, receipt_id)
);

CREATE INDEX IF NOT EXISTS idx_play_receipts_game_time
  ON play_receipts(game_id, mode_id, started_at);
CREATE INDEX IF NOT EXISTS idx_play_receipts_app_time
  ON play_receipts(app_id, started_at);
CREATE INDEX IF NOT EXISTS idx_play_receipts_country_time
  ON play_receipts(country_code, started_at);
CREATE INDEX IF NOT EXISTS idx_play_receipts_player
  ON play_receipts(player_id, app_id, game_id, mode_id);

CREATE TABLE IF NOT EXISTS player_game_stats (
  player_id TEXT NOT NULL,
  app_id TEXT NOT NULL,
  game_id TEXT NOT NULL,
  mode_id TEXT NOT NULL,
  country_code TEXT NOT NULL DEFAULT '',
  play_count INTEGER NOT NULL DEFAULT 0,
  completed_count INTEGER NOT NULL DEFAULT 0,
  first_played_at INTEGER NOT NULL DEFAULT (unixepoch()),
  last_played_at INTEGER NOT NULL DEFAULT (unixepoch()),
  PRIMARY KEY (player_id, app_id, game_id, mode_id)
);

CREATE INDEX IF NOT EXISTS idx_player_game_stats_game
  ON player_game_stats(app_id, game_id, mode_id, play_count DESC);

CREATE TABLE IF NOT EXISTS admin_audit_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  action TEXT NOT NULL,
  game_id TEXT,
  mode_id TEXT,
  app_id TEXT,
  detail TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

-- Existing Yamone Games modes. Keeping both legacy and current IDs avoids breaking
-- an already-installed client while the shared catalog is introduced.
INSERT INTO game_catalog(game_id, mode_id, title, score_unit, max_score) VALUES
  ('ice_jump', 'normal', '빙하 점프', 'centimeters', 10000000),
  ('ice_jump', 'height_cm', '빙하 점프', 'centimeters', 10000000),
  ('fish_munch', 'normal', '물고기 냠냠', 'points', 100000),
  ('fish_munch', 'time_attack', '물고기 냠냠', 'points', 100000),
  ('snow_rush', 'normal', '눈덩이 러쉬', 'milliseconds', 86400),
  ('snow_rush', 'shards_ms', '눈덩이 러쉬', 'milliseconds', 86400000),
  ('orbit_snap', 'normal', '오비트 스냅', 'points', 100000),
  ('color_break', 'normal', '컬러 브레이크', 'points', 100000),
  ('twin_tap', 'normal', '트윈 탭', 'points', 100000),
  ('line_surf', 'normal', '라인 서프', 'points', 100000),
  ('pocket_pulse', 'normal', '포켓 펄스', 'points', 100000),
  ('stack_slice', 'normal', '스택 슬라이스', 'points', 100000)
ON CONFLICT(game_id, mode_id) DO NOTHING;

INSERT INTO app_catalog_settings(app_id, sort_mode)
VALUES ('yamone_arcade2', 'manual')
ON CONFLICT(app_id) DO NOTHING;

INSERT INTO app_games(app_id, game_id, mode_id, enabled, display_order, featured) VALUES
  ('yamone_arcade2', 'orbit_snap', 'normal', 1, 10, 1),
  ('yamone_arcade2', 'color_break', 'normal', 1, 20, 0),
  ('yamone_arcade2', 'twin_tap', 'normal', 1, 30, 0),
  ('yamone_arcade2', 'line_surf', 'normal', 1, 40, 0),
  ('yamone_arcade2', 'pocket_pulse', 'normal', 1, 50, 0),
  ('yamone_arcade2', 'stack_slice', 'normal', 1, 60, 0)
ON CONFLICT(app_id, game_id, mode_id) DO NOTHING;
