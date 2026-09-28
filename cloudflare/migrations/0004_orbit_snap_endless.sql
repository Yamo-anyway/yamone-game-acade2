-- Endless Orbit Snap uses the same game/mode, retaining existing scores and epochs.
UPDATE game_catalog SET max_score = 1000000000
WHERE game_id = 'orbit_snap' AND mode_id = 'normal' AND max_score < 1000000000;
