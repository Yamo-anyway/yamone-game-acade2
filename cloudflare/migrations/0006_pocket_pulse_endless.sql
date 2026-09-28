-- Hide only this app's Line Surf placement; keep the shared game and all history.
UPDATE app_games SET enabled = 0, featured = 0
WHERE app_id = 'yamone_arcade2' AND game_id = 'line_surf';

-- Endless Pocket Pulse keeps its stable game/mode ID and reset epochs.
UPDATE game_catalog SET max_score = MAX(max_score, 1000000000)
WHERE game_id = 'pocket_pulse' AND mode_id = 'normal';
