package com.yamone.arcade2;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;
import com.yamone.arcade2.core.GameId;
import com.yamone.arcade2.core.OrbitEngine;
import com.yamone.arcade2.core.ColorBreakEngine;
import com.yamone.arcade2.core.TwinTapEngine;
import com.yamone.arcade2.data.LocalStore;
import com.yamone.arcade2.data.RankingGateway;
import com.yamone.arcade2.ui.BannerSlot;
import com.yamone.arcade2.ui.OrbitView;
import com.yamone.arcade2.ui.GameView;
import com.yamone.arcade2.ui.ColorBreakView;
import com.yamone.arcade2.ui.TwinTapView;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int BG = 0xFF090E22, PANEL = 0xFF151D35, MINT = 0xFF67E7DB, TEXT = 0xFFF5F7FF, MUTED = 0xFF9AA5C5;
    private LinearLayout root, nav;
    private FrameLayout content;
    private LocalStore store;
    private BannerSlot banner;
    private GameView gameView;
    private GameId activeGame = GameId.ORBIT_SNAP;
    private String screen = "home", runId;
    private final RankingGateway ranking = new RankingGateway.Disabled();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); store = new LocalStore(this);
        root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(i.left, i.top, i.right, i.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        content = new FrameLayout(this); root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        nav = row(); nav.setPadding(dp(12), dp(7), dp(12), dp(7));
        addNav("플레이", this::home); addNav("랭킹", this::rankings); addNav("설정", this::settings);
        root.addView(nav);
        View separator = new View(this); separator.setBackgroundColor(BG); root.addView(separator, new LinearLayout.LayoutParams(-1, dp(12)));
        banner = new BannerSlot(this); root.addView(banner, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root); root.requestApplyInsets(); home();
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
    }
    private void addNav(String title, Runnable action) {
        Button b = button(title, PANEL, TEXT, action); b.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1); lp.setMargins(dp(3), 0, dp(3), 0); nav.addView(b, lp);
    }
    private LinearLayout page(String destination) {
        if (gameView != null) { gameView.setForeground(false); gameView = null; }
        screen = destination; nav.setVisibility(View.VISIBLE); content.removeAllViews();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout page = column(); page.setPadding(dp(22), dp(22), dp(22), dp(18));
        scroll.addView(page); content.addView(scroll); return page;
    }
    private void home() {
        LinearLayout p = page("home");
        p.addView(text("YAMONE / ARCADE 02", 11, MINT)); gap(p, 10);
        p.addView(text("잠깐, 한 판 어때요?", 27, TEXT)); gap(p, 8);
        p.addView(text("60초의 몰입. 손끝으로 만드는 기록.", 13, MUTED)); gap(p, 23);
        LinearLayout featured = column(); featured.setPadding(dp(18), dp(18), dp(18), dp(18));
        featured.setBackground(shape(0xFF183039, 22));
        featured.addView(text("오늘의 도전    ·    ORBIT SNAP", 10, MINT)); gap(featured, 10);
        featured.addView(text("딱, 그 순간에 점프.", 22, TEXT)); gap(featured, 7);
        featured.addView(text("최고 " + store.best(GameId.ORBIT_SNAP) + "점  ·  " + store.plays(GameId.ORBIT_SNAP) + "회 플레이", 12, MUTED)); gap(featured, 15);
        featured.addView(button("오비트 스냅 시작  →", MINT, BG, () -> instructions(GameId.ORBIT_SNAP))); p.addView(featured); gap(p, 23);
        p.addView(text("6개의 작은 도전", 16, TEXT)); gap(p, 12);
        for (GameId game : GameId.values()) {
            LinearLayout card = row(); card.setPadding(dp(15), dp(16), dp(15), dp(16)); card.setBackground(shape(PANEL, 17));
            TextView num = text(String.format(java.util.Locale.ROOT, "%02d", game.ordinal() + 1), 21, game.color);
            card.addView(num, new LinearLayout.LayoutParams(dp(42), -2));
            LinearLayout lines = column(); lines.addView(text(game.title, 17, TEXT)); gap(lines, 5); lines.addView(text(game.tagline, 11, MUTED)); gap(lines, 6); lines.addView(text(game.gesture, 10, game.color));
            card.addView(lines, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(text(game.ready ? "플레이 ›" : "준비 중", 11, game.ready ? MINT : MUTED));
            if (game.ready) { card.setOnClickListener(v -> instructions(game)); card.setContentDescription(game.title + " 시작"); }
            else card.setContentDescription(game.title + ", 준비 중");
            p.addView(card, new LinearLayout.LayoutParams(-1, -2)); gap(p, 9);
        }
    }
    private void instructions(GameId game) {
        String hint = switch (game) {
            case COLOR_BREAK -> "1. 화면 왼쪽 / 오른쪽을 탭해 이동해요.\n2. 아래에서 올라오는 벽 중 내 공과 같은 색·숫자로 통과하세요.\n3. 연속 성공하면 콤보 보너스!\n\n벽이 바뀔 때 내 공 색도 바뀌어요. 통과 +100점, 콤보 보너스 최대 +100점. 3번 실수하거나 60초가 지나면 종료됩니다.";
            case TWIN_TAP -> "1. 점이 아래 판정선에 닿을 때 해당 레인을 탭해요.\n2. 점이 1개면 한쪽, 2개면 양쪽을 함께 누르세요.\n3. 정확할수록 점수가 높고 연속 성공하면 콤보 보너스!\n\n한 손가락을 번갈아 쓰거나 두 손가락을 동시에 사용할 수 있어요. 5번 놓치거나 60초가 지나면 종료됩니다.";
            default -> "1. 화면을 꾹 누르면 점이 원을 돌아요.\n2. 민트 구간에 들어오면 손을 떼세요.\n3. 정확히 맞추면 +150점, 통과하면 +100점!\n\n한 궤도에서 너무 오래 머물면 기회가 줄어요. 3번 실수하거나 60초가 지나면 종료됩니다.";
        };
        new AlertDialog.Builder(this).setTitle(game.title).setMessage(hint)
            .setPositiveButton("시작", (d, w) -> startGame(game)).setNegativeButton("닫기", null).show();
    }
    private void startGame(GameId game) {
        if (!game.ready) return;
        if (gameView != null) gameView.setForeground(false);
        activeGame = game;
        content.removeAllViews(); nav.setVisibility(View.GONE); screen = "game";
        LinearLayout layout = column(); LinearLayout header = row(); header.setPadding(dp(15), dp(8), dp(15), dp(6));
        header.addView(text(game.title, 16, TEXT), new LinearLayout.LayoutParams(0, -2, 1));
        Button pause = button("일시정지", PANEL, MUTED, this::pauseMenu); pause.setTextSize(11); header.addView(pause, new LinearLayout.LayoutParams(dp(98), dp(48)));
        layout.addView(header);
        runId = UUID.randomUUID().toString();
        String currentRun = runId;
        if (game == GameId.COLOR_BREAK) {
            gameView = new ColorBreakView(this, new ColorBreakEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "벽 통과 " + engine.passed() + "회   ·   최고 콤보 " + engine.bestCombo() + "회"));
        } else if (game == GameId.TWIN_TAP) {
            gameView = new TwinTapView(this, new TwinTapEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "성공 " + engine.hits() + "회   ·   동시 성공 " + engine.simultaneousHits() + "회   ·   최고 콤보 " + engine.bestCombo() + "회"));
        } else {
            gameView = new OrbitView(this, new OrbitEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "궤도 통과 " + engine.jumps() + "회   ·   PERFECT " + engine.perfects() + "회"));
        }
        layout.addView(gameView, new LinearLayout.LayoutParams(-1, 0, 1)); content.addView(layout);
    }
    private void pauseMenu() {
        if (gameView == null) return;
        gameView.pauseGame();
        new AlertDialog.Builder(this).setTitle("잠시 쉬어갈까요?").setMessage("계속하면 현재 기록을 이어갑니다.")
            .setPositiveButton("계속", (d, w) -> { if (gameView != null) gameView.resumeGame(); })
            .setNeutralButton("다시 시작", (d, w) -> startGame(activeGame))
            .setNegativeButton("홈으로", (d, w) -> home()).show();
    }
    private void result(String completedRun, GameId game, int score, boolean completed, String detail) {
        if (!"game".equals(screen) || gameView == null || !completedRun.equals(runId)) return;
        boolean record = store.saveResult(game, completedRun, score);
        LinearLayout p = page("result"); gap(p, 25);
        p.addView(text(record ? "NEW BEST!" : "NICE PLAY!", 14, MINT)); gap(p, 17);
        p.addView(text(completed ? "60초, 완주했어요." : "한 번 더 도전해볼까요?", 24, TEXT)); gap(p, 20);
        p.addView(text(Integer.toString(score), 66, TEXT)); p.addView(text(game.title + " · 이번 점수", 12, MUTED)); gap(p, 25);
        p.addView(text(detail, 14, MINT)); gap(p, 13);
        p.addView(text("내 최고기록  " + store.best(game) + "점", 17, TEXT)); gap(p, 30);
        p.addView(button("한 판 더", MINT, BG, () -> startGame(game))); gap(p, 10);
        p.addView(button("게임 고르기", PANEL, TEXT, this::home)); gap(p, 22);
        p.addView(text("기록이 이 기기에 저장됐어요.\n온라인 랭킹은 준비 중입니다.", 12, MUTED));
    }
    private void rankings() {
        LinearLayout p = page("rankings"); p.addView(text("나의 기록", 28, TEXT)); gap(p, 8);
        p.addView(text(store.nickname() + "님의 최고 점수", 13, MUTED)); gap(p, 24);
        for (GameId game : GameId.values()) {
            LinearLayout line = column(); line.setBackground(shape(PANEL, 16)); line.setPadding(dp(17), dp(17), dp(17), dp(17));
            line.addView(text(game.title, 16, game.color)); gap(line, 8);
            line.addView(text(store.plays(game) == 0 ? "아직 기록이 없어요" : store.best(game) + "점", 21, TEXT));
            p.addView(line); gap(p, 10);
        }
        gap(p, 10); p.addView(text("온라인 랭킹", 19, TEXT)); gap(p, 8);
        if (ranking.status() == RankingGateway.Status.NOT_CONNECTED)
            p.addView(text("준비 중입니다. 지금은 내 기기의 최고기록을 볼 수 있어요.", 13, MUTED));
    }
    private void settings() {
        LinearLayout p = page("settings"); p.addView(text("내 플레이 설정", 28, TEXT)); gap(p, 26);
        p.addView(text("닉네임", 14, MINT)); gap(p, 8);
        EditText name = new EditText(this); name.setSingleLine(true); name.setText(store.nickname()); name.setTextColor(TEXT);
        name.setFilters(new InputFilter[] { new InputFilter.LengthFilter(12) }); name.setHint("최대 12자"); name.setHintTextColor(MUTED); p.addView(name); gap(p, 8);
        p.addView(button("닉네임 저장", PANEL, TEXT, () -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { name.setError("닉네임을 입력해주세요"); return; }
            store.nickname(value); android.widget.Toast.makeText(this, "저장했어요", android.widget.Toast.LENGTH_SHORT).show();
        })); gap(p, 24);
        Switch vibration = new Switch(this); vibration.setText("터치 진동"); vibration.setTextColor(TEXT); vibration.setChecked(store.haptics()); vibration.setMinimumHeight(dp(48));
        vibration.setOnCheckedChangeListener((v, enabled) -> store.haptics(enabled)); p.addView(vibration); gap(p, 25);
        p.addView(text("회원가입 없이 바로 플레이해요.\n기록은 이 기기에 저장됩니다. 앱 삭제 시 기록도 사라집니다.", 13, MUTED)); gap(p, 20);
        p.addView(button("내 기록 초기화", PANEL, 0xFFFF84AF, () -> new AlertDialog.Builder(this)
            .setTitle("최고기록을 지울까요?").setMessage("6개 게임의 기기 내 점수와 플레이 횟수가 삭제됩니다. 되돌릴 수 없어요.")
            .setPositiveButton("삭제", (d, w) -> { store.clearScores(); settings(); }).setNegativeButton("취소", null).show()));
        gap(p, 30); p.addView(text("YAMONE ARCADE 2  ·  v" + BuildConfig.VERSION_NAME, 11, MUTED));
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView text(String value, int size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("sans-serif", size >= 16 ? Typeface.BOLD : Typeface.NORMAL)); t.setLineSpacing(dp(3), 1); return t; }
    private Button button(String value, int background, int foreground, Runnable action) { Button b = new Button(this); b.setText(value); b.setTextColor(foreground); b.setAllCaps(false); b.setTextSize(14); b.setMinHeight(dp(52)); b.setBackground(shape(background, 14)); b.setOnClickListener(v -> action.run()); return b; }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private void gap(LinearLayout layout, int height) { layout.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void handleBack() { if ("game".equals(screen)) pauseMenu(); else if (!"home".equals(screen)) home(); else finish(); }
    @SuppressWarnings("deprecation") @Override public void onBackPressed() { handleBack(); }
    @Override protected void onPause() { if (gameView != null) gameView.setForeground(false); if (banner != null) banner.pause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (gameView != null) gameView.setForeground(true); if (banner != null) banner.resume(); }
    @Override protected void onDestroy() { if (banner != null) banner.dispose(); super.onDestroy(); }
}
