-- Rules v2 removes the one-minute limit. Keep the shared game/mode IDs and all
-- player data; only raise the validation ceiling for long Color Break runs.
UPDATE game_catalog SET max_score = 1000000000
WHERE game_id = 'color_break' AND mode_id = 'normal' AND max_score < 1000000000;
