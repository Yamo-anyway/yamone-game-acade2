package com.yamone.arcade2.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.yamone.arcade2.core.GameId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LocalStore {
    private static final String PENDING_PLAYS = "ranking_play_events";
    private final SharedPreferences prefs;

    public static final class PendingPlay {
        public final String eventType, playId;
        public final GameId game;
        public final int score, rankingEpoch;

        PendingPlay(String eventType, String playId, GameId game, int score, int rankingEpoch) {
            this.eventType = eventType;
            this.playId = playId;
            this.game = game;
            this.score = score;
            this.rankingEpoch = Math.max(1, rankingEpoch);
        }

        String encoded() {
            return ("started".equals(eventType) ? "S" : "F") + "|" + playId + "|" + game.key + "|" + score + "|" + rankingEpoch;
        }
    }

    public static final class GameConfig {
        public final GameId game;
        public final boolean enabled, featured;
        public final int displayOrder, rankingEpoch, localResetEpoch;

        public GameConfig(GameId game, boolean enabled, boolean featured, int displayOrder, int rankingEpoch, int localResetEpoch) {
            this.game = game;
            this.enabled = enabled;
            this.featured = featured;
            this.displayOrder = displayOrder;
            this.rankingEpoch = rankingEpoch;
            this.localResetEpoch = localResetEpoch;
        }
    }

    public LocalStore(Context context) {
        prefs = context.getSharedPreferences("arcade2_v1", Context.MODE_PRIVATE);
        if (!prefs.contains("player_id")) prefs.edit().putString("player_id", UUID.randomUUID().toString()).commit();
    }
    public String playerId() { return prefs.getString("player_id", ""); }
    public String nickname() { return prefs.getString("nickname", "플레이어"); }
    public void nickname(String value) { prefs.edit().putString("nickname", value).apply(); }
    public boolean haptics() { return prefs.getBoolean("haptics", true); }
    public void haptics(boolean enabled) { prefs.edit().putBoolean("haptics", enabled).apply(); }
    public int best(GameId game) { return prefs.getInt("best_" + game.key, 0); }
    public int plays(GameId game) { return prefs.getInt("plays_" + game.key, 0); }
    public synchronized boolean saveResult(GameId game, String runId, int score, int rankingEpoch) {
        if (runId == null || runId.isEmpty() || runId.equals(prefs.getString("last_run", ""))) return false;
        int safeScore = Math.max(0, score), previousBest = best(game), previousPlays = plays(game);
        boolean record = safeScore > previousBest;
        Set<String> pending = new HashSet<>(prefs.getStringSet(PENDING_PLAYS, new HashSet<>()));
        pending.add(new PendingPlay("finished", runId, game, safeScore, rankingEpoch).encoded());
        // Results are a terminal event: commit before showing the result screen so process death cannot lose them.
        SharedPreferences.Editor edit = prefs.edit().putString("last_run", runId)
            .putInt("best_" + game.key, Math.max(safeScore, previousBest))
            .putInt("plays_" + game.key, previousPlays + 1).putStringSet(PENDING_PLAYS, pending);
        if (record) edit.putInt("best_epoch_" + game.key, Math.max(1, rankingEpoch));
        edit.commit();
        return record;
    }

    public synchronized void queuePlayStarted(GameId game, String playId, int rankingEpoch) {
        Set<String> pending = new HashSet<>(prefs.getStringSet(PENDING_PLAYS, new HashSet<>()));
        pending.add(new PendingPlay("started", playId, game, 0, rankingEpoch).encoded());
        prefs.edit().putStringSet(PENDING_PLAYS, pending).commit();
    }

    public synchronized List<PendingPlay> pendingPlays() {
        List<PendingPlay> result = new ArrayList<>();
        for (String encoded : prefs.getStringSet(PENDING_PLAYS, new HashSet<>())) {
            String[] parts = encoded.split("\\|", -1);
            if (parts.length != 5) continue;
            GameId game = GameId.fromKey(parts[2]);
            if (game == null || !("S".equals(parts[0]) || "F".equals(parts[0]))) continue;
            try {
                result.add(new PendingPlay("S".equals(parts[0]) ? "started" : "finished", parts[1], game,
                    Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
            } catch (NumberFormatException ignored) { }
        }
        result.sort(Comparator.comparing((PendingPlay event) -> event.playId)
            .thenComparingInt(event -> "started".equals(event.eventType) ? 0 : 1));
        return result;
    }

    public synchronized void clearPendingPlay(PendingPlay event) {
        Set<String> pending = new HashSet<>(prefs.getStringSet(PENDING_PLAYS, new HashSet<>()));
        if (pending.remove(event.encoded())) prefs.edit().putStringSet(PENDING_PLAYS, pending).commit();
    }

    public synchronized void queueRanking(GameId game, int score) {
        int safeScore = Math.max(0, score);
        if (safeScore <= prefs.getInt("ranking_pending_" + game.key, -1)) return;
        prefs.edit().putInt("ranking_pending_" + game.key, safeScore).commit();
    }

    public synchronized int pendingRanking(GameId game) {
        return prefs.getInt("ranking_pending_" + game.key, -1);
    }

    public synchronized void clearPendingRanking(GameId game, int submittedScore) {
        String key = "ranking_pending_" + game.key;
        if (prefs.getInt(key, -1) <= submittedScore) prefs.edit().remove(key).commit();
    }

    public synchronized void requestOnlineDelete() {
        SharedPreferences.Editor edit = prefs.edit().putBoolean("ranking_delete_pending", true).remove(PENDING_PLAYS);
        for (GameId game : GameId.values()) edit.remove("ranking_pending_" + game.key);
        edit.commit();
    }

    public boolean onlineDeletePending() { return prefs.getBoolean("ranking_delete_pending", false); }
    public void onlineDeleteCompleted() { prefs.edit().remove("ranking_delete_pending").commit(); }
    public boolean rankingInitialized() { return prefs.getBoolean("ranking_initialized", false); }
    public void markRankingInitialized() { prefs.edit().putBoolean("ranking_initialized", true).commit(); }

    public synchronized boolean applyCatalog(List<GameConfig> configs) {
        boolean changed = !prefs.getBoolean("catalog_synced", false);
        Set<GameId> configured = new HashSet<>();
        for (GameConfig config : configs) configured.add(config.game);
        SharedPreferences.Editor edit = prefs.edit().putBoolean("catalog_synced", true);
        for (GameId game : GameId.values()) {
            if (!configured.contains(game) && (prefs.getBoolean("game_enabled_" + game.key, false)
                || prefs.getBoolean("game_featured_" + game.key, false))) changed = true;
            edit.putBoolean("game_enabled_" + game.key, false).putBoolean("game_featured_" + game.key, false);
        }
        for (GameConfig config : configs) {
            GameId game = config.game;
            int previousRanking = prefs.getInt("ranking_epoch_" + game.key, -1);
            int previousLocalReset = prefs.getInt("local_reset_epoch_" + game.key, -1);
            if (prefs.getBoolean("game_enabled_" + game.key, !prefs.getBoolean("catalog_synced", false)) != config.enabled
                || prefs.getBoolean("game_featured_" + game.key, false) != config.featured
                || prefs.getInt("game_order_" + game.key, game.ordinal() * 10) != config.displayOrder
                || (previousRanking >= 0 && previousRanking != config.rankingEpoch)
                || (previousLocalReset >= 0 && previousLocalReset != config.localResetEpoch)) changed = true;
            if (previousRanking >= 0 && config.rankingEpoch > previousRanking) edit.remove("ranking_pending_" + game.key);
            if (previousLocalReset >= 0 && config.localResetEpoch > previousLocalReset) {
                edit.remove("best_" + game.key).remove("best_epoch_" + game.key).remove("ranking_pending_" + game.key);
            }
            edit.putBoolean("game_enabled_" + game.key, config.enabled)
                .putBoolean("game_featured_" + game.key, config.featured)
                .putInt("game_order_" + game.key, config.displayOrder)
                .putInt("ranking_epoch_" + game.key, Math.max(1, config.rankingEpoch))
                .putInt("local_reset_epoch_" + game.key, Math.max(0, config.localResetEpoch));
        }
        edit.commit();
        return changed;
    }

    public int rankingEpoch(GameId game) { return prefs.getInt("ranking_epoch_" + game.key, 1); }
    public int bestEpoch(GameId game) { return prefs.getInt("best_epoch_" + game.key, -1); }
    public void markBestEpoch(GameId game, int epoch) { prefs.edit().putInt("best_epoch_" + game.key, Math.max(1, epoch)).commit(); }
    public boolean gameEnabled(GameId game) {
        return !prefs.getBoolean("catalog_synced", false) || prefs.getBoolean("game_enabled_" + game.key, false);
    }
    public List<GameId> visibleGames() {
        List<GameId> games = new ArrayList<>();
        for (GameId game : GameId.values()) if (game.ready && gameEnabled(game)) games.add(game);
        games.sort(Comparator.comparingInt(game -> prefs.getInt("game_order_" + game.key, game.ordinal() * 10)));
        return games;
    }
    public GameId featuredGame() {
        for (GameId game : visibleGames()) if (prefs.getBoolean("game_featured_" + game.key, false)) return game;
        List<GameId> games = visibleGames();
        return games.isEmpty() ? GameId.ORBIT_SNAP : games.get(0);
    }

    public void clearScores() {
        SharedPreferences.Editor edit = prefs.edit();
        for (GameId game : GameId.values()) edit.remove("best_" + game.key).remove("best_epoch_" + game.key).remove("plays_" + game.key);
        edit.remove("last_run").commit();
    }
}
