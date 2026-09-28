-- Display-name change only: preserve twin_tap IDs, records, receipts and reset epochs.
UPDATE game_catalog
SET title = '탭탭', max_score = MAX(max_score, 1000000000)
WHERE game_id = 'twin_tap' AND mode_id = 'normal';
