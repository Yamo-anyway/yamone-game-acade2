package com.yamone.arcade2.data;

import com.yamone.arcade2.core.GameId;
import java.util.Collections;
import java.util.List;

/** Transport boundary only. Never submits until an agreed server implementation replaces Disabled. */
public interface RankingGateway {
    enum Status { NOT_CONNECTED, SUCCESS, RETRYABLE_ERROR, REJECTED }
    final class ScoreSubmission {
        public final String runId, playerId, nickname, rulesVersion = "1", modeId = "60s";
        public final GameId game;
        public final int score;
        public final long durationMs, seed;
        public ScoreSubmission(String runId, String playerId, String nickname, GameId game,
                               int score, long durationMs, long seed) {
            this.runId = runId; this.playerId = playerId; this.nickname = nickname;
            this.game = game; this.score = score; this.durationMs = durationMs; this.seed = seed;
        }
    }
    final class Entry {
        public final int rank, score;
        public final String nickname;
        public Entry(int rank, int score, String nickname) { this.rank = rank; this.score = score; this.nickname = nickname; }
    }
    Status status();
    Status submit(ScoreSubmission score);
    List<Entry> top(GameId game);
    final class Disabled implements RankingGateway {
        public Status status() { return Status.NOT_CONNECTED; }
        public Status submit(ScoreSubmission score) { return Status.NOT_CONNECTED; }
        public List<Entry> top(GameId game) { return Collections.emptyList(); }
    }
}
