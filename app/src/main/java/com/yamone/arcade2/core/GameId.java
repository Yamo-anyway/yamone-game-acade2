package com.yamone.arcade2.core;

public enum GameId {
    ORBIT_SNAP("orbit_snap", "오비트 스냅", "궤도를 돌다가, 딱 맞춰 점프", "길게 누르기 · 손 떼기", 0xFF67D9FF, true),
    COLOR_BREAK("color_break", "컬러 브레이크", "같은 색의 틈으로 통과", "좌우 탭", 0xFFFF84AF, true),
    TWIN_TAP("twin_tap", "트윈 탭", "두 개의 리듬을 동시에", "한 손가락 · 두 손가락", 0xFF73E7B1, false),
    LINE_SURF("line_surf", "라인 서프", "선을 타고, 틈을 넘어", "길게 누르기 · 손 떼기", 0xFFFFB96B, false),
    POCKET_PULSE("pocket_pulse", "포켓 펄스", "두 원이 만나는 순간", "타이밍 탭", 0xFFBB99FF, false),
    STACK_SLICE("stack_slice", "스택 슬라이스", "잘라내고 균형을 지켜", "좌우 스와이프", 0xFF67E7DB, false);

    public final String key, title, tagline, gesture;
    public final int color;
    public final boolean ready;
    GameId(String key, String title, String tagline, String gesture, int color, boolean ready) {
        this.key = key; this.title = title; this.tagline = tagline;
        this.gesture = gesture; this.color = color; this.ready = ready;
    }
}
