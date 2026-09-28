package com.yamone.arcade2.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import com.yamone.arcade2.core.GameId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Live Cloudflare ranking connection with durable best-score retry and app-scoped identity. */
public final class OnlineRankingRepository {
    public static final String API_BASE = "https://yamone-games-ranking-api.yamone-game.workers.dev";
    public static final String APP_ID = "yamone_arcade2";
    private static final int TIMEOUT_MS = 5000;

    private final Context context;
    private final LocalStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private Runnable catalogChangedListener;

    public OnlineRankingRepository(Context context, LocalStore store) {
        this.context = context.getApplicationContext();
        this.store = store;
    }

    public void initialize() {
        if (!store.rankingInitialized()) {
            queueCurrentBests(true);
            store.markRankingInitialized();
        }
        syncPending();
    }

    public void setCatalogChangedListener(Runnable listener) {
        catalogChangedListener = listener;
    }

    public void submitBest(GameId game, int score) {
        if (score <= 0) return;
        store.queueRanking(game, score);
        syncPending();
    }

    public void playStarted(GameId game, String playId, int rankingEpoch) {
        if (playId == null || playId.isEmpty()) return;
        store.queuePlayStarted(game, playId, rankingEpoch);
        syncPending();
    }

    public void nicknameChanged() {
        queueCurrentBests(false);
        syncPending();
    }

    private void queueCurrentBests(boolean migration) {
        for (GameId game : GameId.values()) {
            if (store.plays(game) <= 0 || store.best(game) <= 0) continue;
            if (migration || store.bestEpoch(game) == store.rankingEpoch(game)) {
                store.queueRanking(game, store.best(game));
                if (migration) store.markBestEpoch(game, store.rankingEpoch(game));
            }
        }
    }

    public void deleteAllOnline() {
        store.requestOnlineDelete();
        syncPending();
    }

    public void syncPending() {
        if (closed) return;
        executor.execute(() -> {
            if (!hasUsableNetwork()) return;
            try { syncCatalog(); } catch (IOException ignored) {
                // Older Worker deployments have no catalog endpoint. Ranking retry still continues.
            }
            try {
                if (store.onlineDeletePending()) {
                    deletePlayer();
                    store.onlineDeleteCompleted();
                }
                for (LocalStore.PendingPlay event : store.pendingPlays()) {
                    try {
                        submitPlayEvent(event);
                        store.clearPendingPlay(event);
                    } catch (IOException ignored) {
                        // Keep this and remaining events durable, but do not block legacy best-score retry.
                        break;
                    }
                }
                for (GameId game : GameId.values()) {
                    int score = store.pendingRanking(game);
                    if (score < 0) continue;
                    submit(game, score);
                    store.clearPendingRanking(game, score);
                }
            } catch (IOException ignored) {
                // The durable pending value remains and is retried on resume or the next result.
            }
        });
    }

    public void load(GameId game, RankingGateway.Callback callback) {
        if (closed) return;
        syncPending();
        executor.execute(() -> {
            if (!hasUsableNetwork()) {
                deliver(callback, RankingGateway.Status.OFFLINE, null);
                return;
            }
            try {
                deliver(callback, RankingGateway.Status.SUCCESS, loadBoard(game));
            } catch (IOException ignored) {
                deliver(callback, RankingGateway.Status.SERVER_ERROR, null);
            }
        });
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }

    private void deliver(RankingGateway.Callback callback, RankingGateway.Status status, RankingGateway.Board board) {
        main.post(() -> { if (!closed) callback.complete(status, board); });
    }

    private void submit(GameId game, int score) throws IOException {
        try {
            JSONObject body = new JSONObject();
            body.put("playerId", store.playerId());
            body.put("nickname", store.nickname());
            body.put("countryCode", deviceCountryCode());
            body.put("gameId", game.key);
            body.put("modeId", RankingGateway.MODE_ID);
            body.put("score", score);
            body.put("scoreUnit", RankingGateway.SCORE_UNIT);
            body.put("rankingEpoch", store.rankingEpoch(game));
            request("POST", "/v1/ranking/submit", body);
        } catch (JSONException error) {
            throw new IOException("Unable to encode ranking score", error);
        }
    }

    private void submitPlayEvent(LocalStore.PendingPlay event) throws IOException {
        try {
            JSONObject body = new JSONObject();
            body.put("appId", APP_ID);
            body.put("playId", event.playId);
            body.put("eventType", event.eventType);
            body.put("playerId", store.playerId());
            body.put("nickname", store.nickname());
            body.put("countryCode", deviceCountryCode());
            body.put("gameId", event.game.key);
            body.put("modeId", RankingGateway.MODE_ID);
            body.put("rankingEpoch", event.rankingEpoch);
            if ("finished".equals(event.eventType)) {
                body.put("score", event.score);
                body.put("scoreUnit", RankingGateway.SCORE_UNIT);
            }
            request("POST", "/v1/plays/event", body);
        } catch (JSONException error) {
            throw new IOException("Unable to encode play event", error);
        }
    }

    private void syncCatalog() throws IOException {
        String app = URLEncoder.encode(APP_ID, StandardCharsets.UTF_8.name());
        JSONObject response = request("GET", "/v1/catalog?appId=" + app, null);
        JSONArray games = response.optJSONArray("games");
        if (games == null) throw new IOException("Catalog games missing");
        List<LocalStore.GameConfig> configs = new ArrayList<>();
        for (int index = 0; index < games.length(); index++) {
            JSONObject row = games.optJSONObject(index);
            if (row == null || !RankingGateway.MODE_ID.equals(row.optString("modeId"))) continue;
            GameId game = GameId.fromKey(row.optString("gameId"));
            if (game == null) continue; // A newly registered game needs an app update before it can be shown.
            configs.add(new LocalStore.GameConfig(
                game,
                row.optBoolean("enabled", true),
                row.optBoolean("featured", false),
                row.optInt("displayOrder", game.ordinal() * 10),
                row.optInt("rankingEpoch", 1),
                row.optInt("localResetEpoch", 0)
            ));
        }
        if (store.applyCatalog(configs) && catalogChangedListener != null) {
            main.post(() -> { if (!closed && catalogChangedListener != null) catalogChangedListener.run(); });
        }
    }

    private RankingGateway.Board loadBoard(GameId game) throws IOException {
        String player = URLEncoder.encode(store.playerId(), StandardCharsets.UTF_8.name());
        JSONObject response = request(
            "GET",
            "/v1/ranking/" + game.key + "/" + RankingGateway.MODE_ID + "?playerId=" + player,
            null
        );
        List<RankingGateway.Entry> top = entries(response.optJSONArray("top"));
        List<RankingGateway.Entry> nearby = entries(response.optJSONArray("nearby"));
        RankingGateway.Entry me = entry(response.optJSONObject("me"), true);
        return new RankingGateway.Board(game, response.optInt("totalPlayers", 0), top, me, nearby);
    }

    private List<RankingGateway.Entry> entries(JSONArray array) {
        List<RankingGateway.Entry> result = new ArrayList<>();
        if (array == null) return result;
        for (int index = 0; index < array.length(); index++) {
            RankingGateway.Entry row = entry(array.optJSONObject(index), false);
            if (row != null) result.add(row);
        }
        return result;
    }

    private RankingGateway.Entry entry(JSONObject object, boolean forceMe) {
        if (object == null) return null;
        return new RankingGateway.Entry(
            object.optInt("rank"),
            object.optInt("score"),
            object.optString("nickname"),
            object.optString("countryCode"),
            forceMe || object.optBoolean("isMe", false)
        );
    }

    private void deletePlayer() throws IOException {
        try {
            JSONObject body = new JSONObject().put("playerId", store.playerId());
            request("DELETE", "/v1/ranking/player", body);
        } catch (JSONException error) {
            throw new IOException("Unable to encode ranking deletion", error);
        }
    }

    private JSONObject request(String method, String path, JSONObject body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (body != null) {
            connection.setDoOutput(true);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) { output.write(payload); }
        }
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
            String text = readText(stream);
            if (status < 200 || status >= 300) throw new IOException("Ranking HTTP " + status + ": " + text);
            return text.trim().isEmpty() ? new JSONObject() : new JSONObject(text);
        } catch (JSONException | RuntimeException error) {
            throw new IOException("Invalid ranking response", error);
        } finally {
            connection.disconnect();
        }
    }

    private String readText(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        }
        return text.toString();
    }

    private boolean hasUsableNetwork() {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private String deviceCountryCode() {
        Locale configured = context.getResources().getConfiguration().getLocales().get(0);
        String country = configured.getCountry();
        if (country == null || country.trim().isEmpty()) country = Locale.getDefault().getCountry();
        country = country == null ? "" : country.trim().toUpperCase(Locale.US);
        return country.matches("^[A-Z]{2}$") ? country : "";
    }
}
