package com.yamone.arcade2.core;

public enum GameId {
    ORBIT_SNAP("orbit_snap", "오비트 스냅", "빙글빙글, 반짝이는 순간에 톡", "자동 회전 · 타이밍 탭 · 무제한", 0xFF67D9FF, true),
    COLOR_BREAK("color_break", "컬러 브레이크", "네 가지 색, 끝없이 이어지는 리듬", "4색 탭 · 무제한", 0xFFFF84AF, true),
    TWIN_TAP("twin_tap", "탭탭", "네 줄 위로 반짝이는 리듬", "4레인 탭 · 무제한", 0xFF73E7B1, true),
    LINE_SURF("line_surf", "라인 서프", "선을 타고, 틈을 넘어", "길게 누르기 · 손 떼기", 0xFFFFB96B, true),
    POCKET_PULSE("pocket_pulse", "포켓 펄스", "겹겹이 피어나는 컬러 리듬", "멀티 원 탭 · 무제한", 0xFFBB99FF, true),
    STACK_SLICE("stack_slice", "스택 슬라이스", "잘라내고 균형을 지켜", "좌우 스와이프", 0xFF67E7DB, true);

    public final String key, title, tagline, gesture;
    public final int color;
    public final boolean ready;
    GameId(String key, String title, String tagline, String gesture, int color, boolean ready) {
        this.key = key; this.title = title; this.tagline = tagline;
        this.gesture = gesture; this.color = color; this.ready = ready;
    }

    /** Keep retired IDs readable for existing local and server records. */
    public boolean listed() { return this != LINE_SURF; }

    public static GameId fromKey(String key) {
        if (key == null) return null;
        for (GameId game : values()) if (game.key.equals(key)) return game;
        return null;
    }
}
