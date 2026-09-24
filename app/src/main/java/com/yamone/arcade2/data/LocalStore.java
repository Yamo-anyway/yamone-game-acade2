package com.yamone.arcade2.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.yamone.arcade2.core.GameId;
import java.util.UUID;

public final class LocalStore {
    private final SharedPreferences prefs;
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
    public boolean saveResult(GameId game, String runId, int score) {
        if (runId == null || runId.isEmpty() || runId.equals(prefs.getString("last_run", ""))) return false;
        int safeScore = Math.max(0, score), previousBest = best(game), previousPlays = plays(game);
        boolean record = safeScore > previousBest;
        // Results are a terminal event: commit before showing the result screen so process death cannot lose them.
        prefs.edit().putString("last_run", runId).putInt("best_" + game.key, Math.max(safeScore, previousBest))
            .putInt("plays_" + game.key, previousPlays + 1).commit();
        return record;
    }
    public void clearScores() {
        SharedPreferences.Editor edit = prefs.edit();
        for (GameId game : GameId.values()) edit.remove("best_" + game.key).remove("plays_" + game.key);
        edit.remove("last_run").commit();
    }
}
