package com.yamone.arcade2.data;

import com.yamone.arcade2.core.GameId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Android-independent data contract shared by the live ranking client and rule checks. */
public final class RankingGateway {
    public static final String MODE_ID = "normal";
    public static final String SCORE_UNIT = "points";

    public enum Status { SUCCESS, OFFLINE, SERVER_ERROR }

    public static final class Entry {
        public final int rank, score;
        public final String nickname, countryCode;
        public final boolean isMe;

        public Entry(int rank, int score, String nickname, String countryCode, boolean isMe) {
            this.rank = rank;
            this.score = score;
            this.nickname = nickname == null ? "" : nickname;
            this.countryCode = countryCode == null ? "" : countryCode;
            this.isMe = isMe;
        }
    }

    public static final class Board {
        public final GameId game;
        public final int totalPlayers;
        public final List<Entry> top, nearby;
        public final Entry me;

        public Board(GameId game, int totalPlayers, List<Entry> top, Entry me, List<Entry> nearby) {
            this.game = game;
            this.totalPlayers = Math.max(0, totalPlayers);
            this.top = immutable(top);
            this.me = me;
            this.nearby = immutable(nearby);
        }

        private static List<Entry> immutable(List<Entry> source) {
            if (source == null || source.isEmpty()) return Collections.emptyList();
            return Collections.unmodifiableList(new ArrayList<>(source));
        }
    }

    public interface Callback {
        void complete(Status status, Board board);
    }

    private RankingGateway() {}
}
