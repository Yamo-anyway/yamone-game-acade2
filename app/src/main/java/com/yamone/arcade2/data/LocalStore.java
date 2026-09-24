package com.yamone.arcade2.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.yamone.arcade2.core.GameId;
import java.util.UUID;

public final class LocalStore {
    private final SharedPreferences prefs;
    public LocalStore(Context context) {
        prefs = context.getSharedPreferences("arcade2_v1", Context.MODE_PRIVATE);
        if (!prefs.contains("player_id")) prefs.edit().putString("player_id", UUID.randomUUID().toString()).apply();
    }
    public String playerId() { return prefs.getString("player_id", ""); }
    public String nickname() { return prefs.getString("nickname", "플레이어"); }
    public void nickname(String value) { prefs.edit().putString("nickname", value).apply(); }
    public boolean haptics() { return prefs.getBoolean("haptics", true); }
    public void haptics(boolean enabled) { prefs.edit().putBoolean("haptics", enabled).apply(); }
    public int best(GameId game) { return prefs.getInt("best_" + game.key, 0); }
    public int plays(GameId game) { return prefs.getInt("plays_" + game.key, 0); }
    public boolean saveResult(GameId game, String runId, int score) {
        if (runId.equals(prefs.getString("last_run", ""))) return false;
        boolean record = score > best(game);
        prefs.edit().putString("last_run", runId).putInt("best_" + game.key, Math.max(score, best(game)))
            .putInt("plays_" + game.key, plays(game) + 1).apply();
        return record;
    }
    public void clearScores() {
        SharedPreferences.Editor edit = prefs.edit();
        for (GameId game : GameId.values()) edit.remove("best_" + game.key).remove("plays_" + game.key);
        edit.remove("last_run").apply();
    }
}
