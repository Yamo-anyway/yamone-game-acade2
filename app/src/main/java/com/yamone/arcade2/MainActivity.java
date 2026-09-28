package com.yamone.arcade2;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;
import com.yamone.arcade2.core.GameId;
import com.yamone.arcade2.core.OrbitEngine;
import com.yamone.arcade2.core.ColorBreakEngine;
import com.yamone.arcade2.core.TwinTapEngine;
import com.yamone.arcade2.core.LineSurfEngine;
import com.yamone.arcade2.core.PocketPulseEngine;
import com.yamone.arcade2.core.StackSliceEngine;
import com.yamone.arcade2.data.LocalStore;
import com.yamone.arcade2.data.OnlineRankingRepository;
import com.yamone.arcade2.data.RankingGateway;
import com.yamone.arcade2.ui.BannerSlot;
import com.yamone.arcade2.ui.ArcadeArt;
import com.yamone.arcade2.ui.OrbitView;
import com.yamone.arcade2.ui.GameView;
import com.yamone.arcade2.ui.ColorBreakView;
import com.yamone.arcade2.ui.TwinTapView;
import com.yamone.arcade2.ui.LineSurfView;
import com.yamone.arcade2.ui.PocketPulseView;
import com.yamone.arcade2.ui.StackSliceView;
import java.util.List;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int BG = 0xFFFAF8FF, PANEL = 0xFFFFFFFF, MINT = 0xFF7754AD, TEXT = 0xFF302A43, MUTED = 0xFF6D627D;
    private static final int LILAC = 0xFFF0E9FC, BORDER = 0xFFEDE7F4, PINK = 0xFFAC416D, GAME_BG = 0xFF090E22;
    private static final String STATE_SCREEN = "screen", STATE_GAME = "active_game", STATE_ACTIVE_RUN = "active_run";
    private LinearLayout root, nav;
    private FrameLayout content;
    private LocalStore store;
    private BannerSlot banner;
    private GameView gameView;
    private GameId activeGame = GameId.ORBIT_SNAP;
    private GameId rankingGame = GameId.ORBIT_SNAP;
    private String screen = "home", runId;
    private int runRankingEpoch = 1;
    private OnlineRankingRepository ranking;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LocalStore(this);
        ranking = new OnlineRankingRepository(this, store);
        root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(i.left, i.top, i.right, i.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        content = new FrameLayout(this); root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        nav = row(); nav.setPadding(dp(14), dp(8), dp(14), dp(8));
        nav.setBackground(shape(PANEL, 26)); nav.setElevation(dp(5));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, -2);
        navParams.setMargins(dp(16), dp(4), dp(16), dp(8)); root.addView(nav, navParams);
        banner = new BannerSlot(this); root.addView(banner, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root); root.requestApplyInsets();
        ranking.setCatalogChangedListener(() -> {
            if ("home".equals(screen)) home();
            else if ("rankings".equals(screen)) rankings();
        });
        restoreDestination(savedInstanceState);
        ranking.initialize();
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
    }
    private void restoreDestination(Bundle state) {
        if (state == null) { home(); return; }
        String gameName = state.getString(STATE_GAME, GameId.ORBIT_SNAP.name());
        try { activeGame = GameId.valueOf(gameName); } catch (IllegalArgumentException ignored) { activeGame = GameId.ORBIT_SNAP; }
        String restoredScreen = state.getString(STATE_SCREEN, "home");
        if (state.getBoolean(STATE_ACTIVE_RUN, false)) {
            home();
            root.post(() -> new AlertDialog.Builder(this).setTitle("진행 중이던 판이 종료됐어요")
                .setMessage("앱이 다시 시작되어 진행 중 기록은 저장하지 않았습니다. 같은 게임을 새로 시작할 수 있어요.")
                .setPositiveButton("다시 시작", (d, w) -> startGame(activeGame)).setNegativeButton("홈", null).show());
        } else if ("rankings".equals(restoredScreen) || "result".equals(restoredScreen)) rankings();
        else if ("settings".equals(restoredScreen)) settings();
        else home();
    }
    private void addNav(String title, String destination, ArcadeArt.Symbol symbol, Runnable action) {
        boolean selected = destination.equals(screen) || ("home".equals(destination) && "result".equals(screen));
        LinearLayout tab = column(); tab.setGravity(Gravity.CENTER); tab.setPadding(dp(4), dp(5), dp(4), dp(5));
        tab.addView(new ArcadeArt(this, symbol, selected ? MINT : MUTED), new LinearLayout.LayoutParams(dp(25), dp(25)));
        TextView label = text(title, 11, selected ? MINT : MUTED); label.setGravity(Gravity.CENTER); tab.addView(label);
        tab.setBackground(ripple(selected ? LILAC : PANEL, 18)); tab.setSelected(selected);
        tab.setContentDescription(title + (selected ? ", 선택됨" : "")); tab.setFocusable(true); tab.setMinimumHeight(dp(56));
        tab.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1); lp.setMargins(dp(3), 0, dp(3), 0); nav.addView(tab, lp);
    }
    private void updateNav() {
        nav.removeAllViews();
        addNav("홈", "home", ArcadeArt.Symbol.HOME, this::home);
        addNav("랭킹", "rankings", ArcadeArt.Symbol.TROPHY, this::rankings);
        addNav("설정", "settings", ArcadeArt.Symbol.SETTINGS, this::settings);
    }
    private LinearLayout page(String destination) {
        if (gameView != null) { gameView.setForeground(false); gameView = null; }
        screen = destination; nav.setVisibility(View.VISIBLE); content.removeAllViews(); updateNav();
        root.setBackgroundColor(BG); systemBars(false);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout page = column(); page.setPadding(dp(22), dp(20), dp(22), dp(20));
        scroll.addView(page); content.addView(scroll); return page;
    }
    private void home() {
        LinearLayout p = page("home");
        GameId featuredGame = store.featuredGame();
        List<GameId> visibleGames = store.visibleGames();
        LinearLayout brand = row(); LinearLayout wordmark = column();
        TextView logo = text("yamone", 24, TEXT); logo.setLetterSpacing(-.04f); wordmark.addView(logo);
        TextView caption = text("작고 반짝이는 오락실", 10, MUTED); wordmark.addView(caption);
        brand.addView(wordmark, new LinearLayout.LayoutParams(0, -2, 1));
        FrameLayout profile = new FrameLayout(this); profile.setBackground(ripple(LILAC, 20));
        profile.addView(new ArcadeArt(this, ArcadeArt.Symbol.AVATAR, MINT), new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER));
        profile.setContentDescription("내 프로필과 설정"); profile.setFocusable(true); profile.setOnClickListener(v -> settings());
        brand.addView(profile, new LinearLayout.LayoutParams(dp(52), dp(52))); p.addView(brand); gap(p, 24);
        p.addView(text("오늘도, 가볍게 한 판 ✦", compactCards() ? 23 : 26, TEXT)); gap(p, 7);
        p.addView(text("손끝에서 시작되는 기분 좋은 60초", 13, MUTED)); gap(p, 20);
        if (!visibleGames.isEmpty()) {
            LinearLayout featured = panel(0xFFF0E7FF, 22); featured.setBackground(gradient(0xFFECE3FF, 0xFFFFE9F1, 28));
            featured.addView(chip("✦  오늘의 추천", MINT, 0xBFFFFFFF)); gap(featured, 8);
            LinearLayout scene = compactCards() ? column() : row(); LinearLayout intro = column();
            intro.addView(text(featuredGame.title, 23, TEXT)); gap(intro, 7);
            intro.addView(text(featuredGame.tagline, 12, MUTED)); gap(intro, 12);
            intro.addView(text("MY BEST  " + number(store.best(featuredGame)), 11, MINT));
            scene.addView(intro, compactCards() ? new LinearLayout.LayoutParams(-1, -2) : new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams featuredArt = new LinearLayout.LayoutParams(dp(compactCards() ? 80 : 100), dp(compactCards() ? 74 : 120));
            featuredArt.gravity = Gravity.CENTER_HORIZONTAL;
            scene.addView(new ArcadeArt(this, featuredGame, true), featuredArt);
            featured.addView(scene); gap(featured, 8);
            featured.addView(button("지금 플레이  →", MINT, PANEL, () -> instructions(featuredGame)));
            p.addView(featured); gap(p, 25);
        }
        section(p, "작은 게임 상자", visibleGames.size() + " GAMES"); gap(p, 13);
        int columns = compactCards() ? 1 : 2;
        for (int index = 0; index < visibleGames.size(); index += columns) {
            LinearLayout tiles = row(); tiles.setGravity(Gravity.TOP);
            for (int col = 0; col < columns; col++) {
                int item = index + col;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
                lp.setMargins(col == 0 ? 0 : dp(6), 0, col == columns - 1 ? 0 : dp(6), 0);
                tiles.addView(item < visibleGames.size() ? gameCard(visibleGames.get(item)) : new View(this), lp);
            }
            p.addView(tiles); gap(p, 12);
        }
        if (visibleGames.isEmpty()) messageCard(p, "잠시 쉬어가는 중이에요", "새로운 도전을 준비하고 있어요. 조금 후 다시 만나요.", ArcadeArt.Symbol.SPARKLE);
    }
    private LinearLayout gameCard(GameId game) {
        LinearLayout card = panel(PANEL, 14); card.setBackground(ripple(PANEL, 24)); card.setElevation(dp(1));
        FrameLayout art = new FrameLayout(this); art.setBackground(shape(ArcadeArt.tint(game), 18));
        art.addView(new ArcadeArt(this, game, true), new FrameLayout.LayoutParams(dp(100), dp(100), Gravity.CENTER));
        card.addView(art, new LinearLayout.LayoutParams(-1, dp(102))); gap(card, 12);
        card.addView(text(game.title, 16, TEXT)); gap(card, 5);
        TextView gesture = text(game.gesture, 11, MUTED); gesture.setMinLines(2); card.addView(gesture); gap(card, 8);
        LinearLayout footer = row(); footer.addView(text(number(store.best(game)) + "점", 12, ArcadeArt.accent(game)), new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(text("↗", 20, ArcadeArt.accent(game))); card.addView(footer);
        card.setContentDescription(game.title + ", " + game.gesture + ", 최고 " + store.best(game) + "점, 시작");
        card.setFocusable(true); card.setOnClickListener(v -> instructions(game)); return card;
    }
    private void instructions(GameId game) {
        String hint = switch (game) {
            case COLOR_BREAK -> "1. 화면 왼쪽 / 오른쪽을 탭해 이동해요.\n2. 아래에서 올라오는 벽 중 내 공과 같은 색·숫자로 통과하세요.\n3. 연속 성공하면 콤보 보너스!\n\n벽이 바뀔 때 내 공 색도 바뀌어요. 통과 +100점, 콤보 보너스 최대 +100점. 3번 실수하거나 60초가 지나면 종료됩니다.";
            case TWIN_TAP -> "1. 점이 아래 판정선에 닿을 때 해당 레인을 탭해요.\n2. 점이 1개면 한쪽, 2개면 양쪽을 함께 누르세요.\n3. 정확할수록 점수가 높고 연속 성공하면 콤보 보너스!\n\n한 손가락을 번갈아 쓰거나 두 손가락을 동시에 사용할 수 있어요. 5번 놓치거나 60초가 지나면 종료됩니다.";
            case LINE_SURF -> "1. 화면을 누르고 있으면 선을 타고 달려요.\n2. 틈이나 장애물 앞에서 손을 떼면 점프해요.\n3. 착지한 뒤 다시 누르고, 다음 장애물 앞에서 떼세요.\n\n통과할수록 점수와 연속 보너스가 쌓여요. 3번 부딪히거나 60초가 지나면 종료됩니다.";
            case POCKET_PULSE -> "1. 화면을 한 번 탭하면 파동이 시작돼요.\n2. 중심에서 커지는 파란 파동이 보라 목표 링과 겹칠 때 탭하세요.\n3. 오차가 작을수록 PERFECT·GREAT·GOOD 점수가 높아져요.\n\n연속 성공하면 콤보 보너스가 쌓여요. 4번 놓치거나 60초가 지나면 종료됩니다.";
            case STACK_SLICE -> "1. 위에서 움직이는 블록의 튀어나온 쪽을 확인하세요.\n2. 잘라낼 면을 향해 왼쪽 또는 오른쪽으로 스와이프하세요.\n3. 남은 부분이 쌓이며, 무게중심이 지지면을 벗어나면 무너져요.\n\n중앙에 가깝게 쌓을수록 균형 보너스가 커져요. 블록당 제한시간이 지나거나 60초가 되면 종료됩니다.";
            default -> "1. 화면을 꾹 누르면 점이 원을 돌아요.\n2. 민트 구간에 들어오면 손을 떼세요.\n3. 정확히 맞추면 +150점, 통과하면 +100점!\n\n한 궤도에서 너무 오래 머물면 기회가 줄어요. 3번 실수하거나 60초가 지나면 종료됩니다.";
        };
        new AlertDialog.Builder(this).setTitle(game.title).setMessage(hint)
            .setPositiveButton("시작", (d, w) -> startGame(game)).setNegativeButton("닫기", null).show();
    }
    private void startGame(GameId game) {
        if (!game.ready || !store.gameEnabled(game)) return;
        if (gameView != null) gameView.setForeground(false);
        activeGame = game;
        content.removeAllViews(); nav.setVisibility(View.GONE); screen = "game";
        root.setBackgroundColor(GAME_BG); systemBars(true);
        LinearLayout layout = column(); LinearLayout header = row(); header.setPadding(dp(15), dp(8), dp(15), dp(6));
        header.addView(text(game.title, 16, 0xFFF5F7FF), new LinearLayout.LayoutParams(0, -2, 1));
        Button pause = button("일시정지", 0xFF151D35, 0xFFCFD7EF, this::pauseMenu); pause.setTextSize(11); header.addView(pause, new LinearLayout.LayoutParams(dp(98), dp(48)));
        layout.addView(header);
        runId = UUID.randomUUID().toString();
        String currentRun = runId;
        runRankingEpoch = store.rankingEpoch(game);
        ranking.playStarted(game, currentRun, runRankingEpoch);
        if (game == GameId.COLOR_BREAK) {
            gameView = new ColorBreakView(this, new ColorBreakEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "벽 통과 " + engine.passed() + "회   ·   최고 콤보 " + engine.bestCombo() + "회"));
        } else if (game == GameId.TWIN_TAP) {
            gameView = new TwinTapView(this, new TwinTapEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "성공 " + engine.hits() + "회   ·   동시 성공 " + engine.simultaneousHits() + "회   ·   최고 콤보 " + engine.bestCombo() + "회"));
        } else if (game == GameId.LINE_SURF) {
            gameView = new LineSurfView(this, new LineSurfEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "거리 " + engine.distanceMeters() + "m   ·   장애물 통과 " + engine.cleared() + "회   ·   점프 " + engine.jumps() + "회"));
        } else if (game == GameId.POCKET_PULSE) {
            gameView = new PocketPulseView(this, new PocketPulseEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "성공 " + engine.hits() + "회   ·   PERFECT " + engine.perfects() + "회   ·   최고 콤보 " + engine.bestCombo() + "회"));
        } else if (game == GameId.STACK_SLICE) {
            gameView = new StackSliceView(this, new StackSliceEngine(System.nanoTime()), store.haptics(), engine ->
                result(currentRun, game, engine.score(), engine.remaining() == 0,
                    "적층 " + engine.placed() + "개   ·   균형 성공 " + engine.balanced() + "회   ·   최고 연속 " + engine.bestBalanceStreak() + "회"));
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
        boolean record = store.saveResult(game, completedRun, score, runRankingEpoch);
        if (record) ranking.submitBest(game, store.best(game));
        else ranking.syncPending();
        LinearLayout p = page("result"); gap(p, 25);
        p.addView(new ArcadeArt(this, ArcadeArt.Symbol.TROPHY, 0xFFBD8734), new LinearLayout.LayoutParams(dp(90), dp(90)));
        p.addView(text(record ? "✦  NEW BEST!" : "✦  NICE PLAY!", 14, MINT)); gap(p, 17);
        p.addView(text(completed ? "60초, 완주했어요." : "한 번 더 도전해볼까요?", 24, TEXT)); gap(p, 20);
        p.addView(text(number(score), 58, TEXT)); p.addView(text(game.title + " · 이번 점수", 12, MUTED)); gap(p, 25);
        p.addView(text(detail, 14, MINT)); gap(p, 13);
        p.addView(text("내 최고기록  " + store.best(game) + "점", 17, TEXT)); gap(p, 30);
        p.addView(button("한 판 더", MINT, BG, () -> startGame(game))); gap(p, 10);
        p.addView(button("게임 고르기", PANEL, TEXT, this::home)); gap(p, 22);
        p.addView(text(record
            ? "새 최고기록이 저장됐어요. 인터넷 연결 시 온라인 랭킹에도 반영됩니다."
            : "기기와 온라인 랭킹에는 게임별 최고기록 한 개만 유지됩니다.", 12, MUTED));
    }
    private void rankings() {
        rankings(rankingGame);
    }
    private void rankings(GameId selected) {
        List<GameId> visibleGames = store.visibleGames();
        if (visibleGames.isEmpty()) { home(); return; }
        if (!visibleGames.contains(selected)) selected = visibleGames.get(0);
        final GameId selectedGame = selected;
        rankingGame = selected;
        LinearLayout p = page("rankings");
        pageTitle(p, "HALL OF FAME", "반짝이는 기록들", "한 판의 즐거움이, 나만의 순위로.", ArcadeArt.Symbol.TROPHY);
        HorizontalScrollView picker = new HorizontalScrollView(this); picker.setHorizontalScrollBarEnabled(false);
        LinearLayout choices = row(); Button selectedOption = null;
        for (GameId game : visibleGames) {
            Button option = button(game.title, game == selectedGame ? MINT : PANEL, game == selectedGame ? PANEL : MUTED, () -> rankings(game));
            option.setTextSize(12); option.setSelected(game == selectedGame);
            option.setContentDescription(game.title + (game == selectedGame ? ", 선택됨" : " 랭킹"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2); params.setMargins(0, 0, dp(8), 0);
            choices.addView(option, params); if (game == selectedGame) selectedOption = option;
        }
        picker.addView(choices); p.addView(picker);
        final Button activeOption = selectedOption;
        picker.post(() -> { if (activeOption != null) picker.scrollTo(Math.max(0, activeOption.getLeft() - dp(20)), 0); });
        gap(p, 18);
        LinearLayout local = panel(ArcadeArt.tint(selectedGame), 20); LinearLayout record = row(); LinearLayout recordText = column();
        recordText.addView(text(selectedGame.title + " · 내 최고기록", 12, ArcadeArt.accent(selectedGame))); gap(recordText, 8);
        recordText.addView(text(store.plays(selectedGame) == 0 ? "첫 기록을 기다려요" : number(store.best(selectedGame)) + "점", compactCards() ? 20 : 25, TEXT)); gap(recordText, 5);
        recordText.addView(text("이 기기에서 " + number(store.plays(selectedGame)) + "회 플레이", 11, MUTED));
        record.addView(recordText, new LinearLayout.LayoutParams(0, -2, 1));
        if (!compactCards()) record.addView(new ArcadeArt(this, selectedGame, true), new LinearLayout.LayoutParams(dp(64), dp(76))); local.addView(record);
        p.addView(local); gap(p, 20);
        LinearLayout online = column(); online.setTag("onlineRanking");
        messageCard(online, "기록을 모으고 있어요", "온라인 순위를 불러오는 중…", ArcadeArt.Symbol.SPARKLE);
        p.addView(online);
        ranking.load(selectedGame, (status, board) -> {
            if (!"rankings".equals(screen) || rankingGame != selectedGame) return;
            renderOnlineRanking(online, status, board);
        });
    }
    private void renderOnlineRanking(LinearLayout target, RankingGateway.Status status, RankingGateway.Board board) {
        target.removeAllViews();
        if (status == RankingGateway.Status.OFFLINE) {
            messageCard(target, "잠깐, 연결을 기다려요", "내 기록은 기기에 남아 있어요.\n인터넷에 연결되면 순위를 확인할 수 있어요.", ArcadeArt.Symbol.SPARKLE);
            gap(target, 10); target.addView(button("다시 연결하기", LILAC, MINT, () -> rankings(rankingGame)));
            return;
        }
        if (status != RankingGateway.Status.SUCCESS || board == null) {
            messageCard(target, "기록이 잠시 쉬고 있어요", "온라인 순위를 불러오지 못했어요.\n잠시 후 다시 시도해주세요.", ArcadeArt.Symbol.SPARKLE); gap(target, 10);
            target.addView(button("다시 시도", LILAC, MINT, () -> rankings(rankingGame)));
            return;
        }
        section(target, "명예의 전당", number(board.totalPlayers) + "명 참여"); gap(target, 14);
        if (!board.top.isEmpty()) { podium(target, board.top); gap(target, 16); }
        if (board.me != null) {
            target.addView(text("MY RANK  ·  나의 현재 순위", 11, MINT)); gap(target, 8);
            target.addView(rankingRow(board.me, true)); gap(target, 18);
        }
        if (board.top.isEmpty()) {
            messageCard(target, "첫 번째 별이 되어주세요", "아직 등록된 기록이 없어요.\n첫 순위의 주인공은 바로 나!", ArcadeArt.Symbol.TROPHY);
            gap(target, 12); target.addView(button("첫 기록 만들기  →", MINT, PANEL, () -> instructions(rankingGame)));
            return;
        }
        section(target, "전체 순위", "TOP 100"); gap(target, 10);
        for (RankingGateway.Entry entry : board.top) {
            target.addView(rankingRow(entry, entry.isMe)); gap(target, 5);
        }
        if (board.me != null && board.me.rank > board.top.size() && !board.nearby.isEmpty()) {
            gap(target, 15); target.addView(text("내 순위 주변", 17, TEXT)); gap(target, 8);
            for (RankingGateway.Entry entry : board.nearby) {
                target.addView(rankingRow(entry, entry.isMe)); gap(target, 5);
            }
        }
    }
    private LinearLayout rankingRow(RankingGateway.Entry entry, boolean highlight) {
        LinearLayout line = row(); line.setPadding(dp(14), dp(15), dp(14), dp(15));
        line.setBackground(shape(highlight ? LILAC : PANEL, 18));
        TextView place = text(String.format(java.util.Locale.ROOT, "%02d", entry.rank), 16, highlight ? MINT : MUTED);
        line.addView(place, new LinearLayout.LayoutParams(dp(42), -2));
        LinearLayout identity = column(); identity.addView(text(entry.nickname + (highlight ? " · 나" : ""), 14, TEXT));
        String country = countryFlag(entry.countryCode);
        identity.addView(text(country.isEmpty() ? "국가 미설정" : country + entry.countryCode, 10, MUTED));
        line.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        TextView score = text(number(entry.score) + "점", 15, highlight ? MINT : TEXT); score.setPadding(dp(6), 0, 0, 0); line.addView(score);
        return line;
    }
    private void podium(LinearLayout target, List<RankingGateway.Entry> entries) {
        LinearLayout podium = row(); podium.setGravity(Gravity.BOTTOM);
        int count = Math.min(3, entries.size());
        int[] order = count == 3 ? new int[] {1, 0, 2} : count == 2 ? new int[] {1, 0} : new int[] {0};
        for (int index : order) {
            RankingGateway.Entry entry = entries.get(index); boolean winner = entry.rank == 1;
            LinearLayout tile = panel(winner ? 0xFFFFF1D8 : index == 1 ? 0xFFEFEAF9 : 0xFFFFEAE3, 10); tile.setGravity(Gravity.CENTER);
            tile.addView(new ArcadeArt(this, ArcadeArt.Symbol.TROPHY, winner ? 0xFFAE7827 : index == 1 ? 0xFF88729F : 0xFFB87758), new LinearLayout.LayoutParams(dp(40), dp(winner ? 53 : 40)));
            TextView rank = text(entry.rank + "위", 13, TEXT); rank.setGravity(Gravity.CENTER); tile.addView(rank); gap(tile, 6);
            TextView name = text(entry.nickname, 12, TEXT); name.setGravity(Gravity.CENTER); name.setMaxLines(2); name.setEllipsize(android.text.TextUtils.TruncateAt.END); tile.addView(name);
            TextView score = text(number(entry.score), 15, TEXT); score.setGravity(Gravity.CENTER); tile.addView(score);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1); lp.setMargins(dp(3), 0, dp(3), 0); podium.addView(tile, lp);
        }
        target.addView(podium);
    }
    private String countryFlag(String countryCode) {
        String code = countryCode == null ? "" : countryCode.trim().toUpperCase(java.util.Locale.US);
        if (!code.matches("^[A-Z]{2}$")) return "";
        int first = 0x1F1E6 + code.charAt(0) - 'A';
        int second = 0x1F1E6 + code.charAt(1) - 'A';
        return new String(Character.toChars(first)) + new String(Character.toChars(second)) + "  ";
    }
    private void settings() {
        LinearLayout p = page("settings");
        pageTitle(p, "MY LITTLE ARCADE", "나의 작은 취향", "내게 꼭 맞는 플레이를 준비해요.", ArcadeArt.Symbol.SETTINGS);
        LinearLayout profile = panel(LILAC, 20); profile.setBackground(gradient(0xFFEDE5FC, 0xFFFFECF3, 28));
        LinearLayout identity = row(); identity.addView(new ArcadeArt(this, ArcadeArt.Symbol.AVATAR, MINT), new LinearLayout.LayoutParams(dp(compactCards() ? 48 : 70), dp(compactCards() ? 56 : 76)));
        LinearLayout greeting = column(); greeting.setPadding(dp(15), 0, 0, 0);
        greeting.addView(text("반가워요!", 12, MINT)); gap(greeting, 4);
        TextView nickname = text(store.nickname() + " 님", compactCards() ? 18 : 22, TEXT); nickname.setMaxLines(2); nickname.setEllipsize(android.text.TextUtils.TruncateAt.END); greeting.addView(nickname);
        identity.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1)); profile.addView(identity); gap(profile, 16);
        int plays = 0; for (GameId game : GameId.values()) plays += store.plays(game);
        profile.addView(chip("✦  지금까지 " + number(plays) + "번의 작은 도전", MINT, 0xCCFFFFFF)); p.addView(profile); gap(p, 22);
        section(p, "나의 프로필", "PROFILE"); gap(p, 12);
        LinearLayout edit = panel(PANEL, 18); edit.addView(text("랭킹에 표시할 닉네임", 13, TEXT)); gap(edit, 10);
        EditText name = new EditText(this); name.setSingleLine(true); name.setText(store.nickname()); name.setTextColor(TEXT);
        name.setTextSize(16); name.setPadding(dp(14), dp(12), dp(14), dp(12)); name.setBackground(shape(BG, 14)); name.setMinimumHeight(dp(52));
        name.setContentDescription("랭킹 닉네임, 최대 12자"); name.setSelectAllOnFocus(true);
        name.setFilters(new InputFilter[] { new InputFilter.LengthFilter(12) }); name.setHint("최대 12자"); name.setHintTextColor(MUTED); edit.addView(name); gap(edit, 10);
        edit.addView(button("닉네임 저장하기", LILAC, MINT, () -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { name.setError("닉네임을 입력해주세요"); return; }
            store.nickname(value); ranking.nicknameChanged();
            nickname.setText(value + " 님"); name.clearFocus();
            android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(name.getWindowToken(), 0);
            android.widget.Toast.makeText(this, "닉네임과 랭킹 정보를 갱신했어요", android.widget.Toast.LENGTH_SHORT).show();
        })); p.addView(edit); gap(p, 22);
        section(p, "플레이 취향", "PLAY"); gap(p, 12);
        LinearLayout preferences = panel(PANEL, 18);
        Switch vibration = new Switch(this); vibration.setText("손끝에 톡, 터치 진동"); vibration.setTextSize(14); vibration.setTextColor(TEXT); vibration.setChecked(store.haptics()); vibration.setMinimumHeight(dp(52));
        vibration.setThumbTintList(new ColorStateList(new int[][] { new int[] {android.R.attr.state_checked}, new int[] {} }, new int[] {MINT, 0xFFB8AECA}));
        vibration.setTrackTintList(new ColorStateList(new int[][] { new int[] {android.R.attr.state_checked}, new int[] {} }, new int[] {0xFFD8C8F0, BORDER}));
        vibration.setOnCheckedChangeListener((v, enabled) -> store.haptics(enabled)); preferences.addView(vibration);
        preferences.addView(text("정확히 맞춘 순간을 진동으로 느껴보세요.", 11, MUTED)); p.addView(preferences); gap(p, 22);
        section(p, "기록과 개인정보", "RECORDS"); gap(p, 12);
        LinearLayout privacy = panel(PANEL, 18);
        privacy.addView(text("가입 없이, 나만의 기록", 15, TEXT)); gap(privacy, 8);
        privacy.addView(text("앱마다 별도의 플레이어 ID를 사용해요. 닉네임·국가 코드·게임별 점수와 플레이 횟수를 랭킹 및 통계에 사용합니다. 앱을 삭제하면 기기 기록과 ID는 복구할 수 없어요.", 12, MUTED)); gap(privacy, 18);
        privacy.addView(button("내 기록 초기화", 0xFFFFEDF1, PINK, () -> new AlertDialog.Builder(this)
            .setTitle("모든 기록을 지울까요?").setMessage("6개 게임의 기기 기록과 이 앱의 온라인 랭킹·플레이 통계를 삭제합니다. 오프라인이면 연결될 때 서버 삭제를 완료합니다. 삭제한 기록은 되돌릴 수 없어요.")
            .setPositiveButton("삭제", (d, w) -> {
                ranking.deleteAllOnline(); store.clearScores(); settings();
                android.widget.Toast.makeText(this, "기록 삭제 요청을 저장했어요", android.widget.Toast.LENGTH_SHORT).show();
            }).setNegativeButton("취소", null).show())); p.addView(privacy);
        gap(p, 25); TextView version = text("작은 즐거움을 모아요 ✦\nyamone arcade 2  ·  v" + BuildConfig.VERSION_NAME, 11, MUTED); version.setGravity(Gravity.CENTER); p.addView(version);
    }
    private void pageTitle(LinearLayout page, String eyebrow, String title, String subtitle, ArcadeArt.Symbol symbol) {
        LinearLayout header = row(); LinearLayout words = column(); words.addView(text(eyebrow, 10, MINT)); gap(words, 9);
        if (!compactCards()) words.addView(text(title, 27, TEXT)); header.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        ArcadeArt icon = new ArcadeArt(this, symbol, symbol == ArcadeArt.Symbol.TROPHY ? 0xFFAD7932 : MINT);
        icon.setBackground(shape(symbol == ArcadeArt.Symbol.TROPHY ? 0xFFFFF0D9 : LILAC, 22)); icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        header.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54))); page.addView(header); gap(page, 10);
        if (compactCards()) { page.addView(text(title, 24, TEXT)); gap(page, 10); }
        page.addView(text(subtitle, 13, MUTED)); gap(page, 24);
    }
    private void section(LinearLayout page, String title, String detail) {
        LinearLayout line = row(); line.addView(text(title, 18, TEXT), new LinearLayout.LayoutParams(0, -2, 1));
        TextView subtitle = text(detail, 10, MUTED); subtitle.setPadding(dp(8), 0, 0, 0); line.addView(subtitle); page.addView(line);
    }
    private void messageCard(LinearLayout page, String title, String subtitle, ArcadeArt.Symbol symbol) {
        LinearLayout card = panel(PANEL, 23); card.setGravity(Gravity.CENTER);
        card.addView(new ArcadeArt(this, symbol, MINT), new LinearLayout.LayoutParams(dp(56), dp(56))); gap(card, 9);
        TextView heading = text(title, 17, TEXT); heading.setGravity(Gravity.CENTER); card.addView(heading); gap(card, 9);
        TextView body = text(subtitle, 12, MUTED); body.setGravity(Gravity.CENTER); card.addView(body); page.addView(card);
    }
    private LinearLayout panel(int color, int padding) {
        LinearLayout panel = column(); panel.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        panel.setBackground(shape(color, 24)); return panel;
    }
    private TextView chip(String value, int foreground, int background) {
        TextView t = text(value, 11, foreground); t.setPadding(dp(12), dp(7), dp(12), dp(7)); t.setBackground(shape(background, 20));
        t.setLayoutParams(new LinearLayout.LayoutParams(-2, -2)); return t;
    }
    private boolean compactCards() { return getResources().getConfiguration().screenWidthDp < 360 || getResources().getConfiguration().fontScale > 1.25f; }
    private String number(int value) { return String.format(java.util.Locale.getDefault(), "%,d", value); }
    @SuppressWarnings("deprecation") private void systemBars(boolean dark) {
        getWindow().setStatusBarColor(dark ? GAME_BG : BG); getWindow().setNavigationBarColor(dark ? GAME_BG : BG);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView text(String value, int size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create(size >= 16 ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL)); t.setLineSpacing(dp(3), 1); return t; }
    private Button button(String value, int background, int foreground, Runnable action) { Button b = new Button(this); b.setText(value); b.setTextColor(foreground); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); b.setMinHeight(dp(52)); b.setMinimumHeight(dp(52)); b.setPadding(dp(16), dp(12), dp(16), dp(12)); b.setBackgroundTintList(null); b.setBackground(ripple(background, 18)); b.setStateListAnimator(null); b.setOnClickListener(v -> action.run()); return b; }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private GradientDrawable gradient(int start, int end, int radius) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {start, end}); d.setCornerRadius(dp(radius)); return d; }
    private RippleDrawable ripple(int color, int radius) { return new RippleDrawable(ColorStateList.valueOf(0x227754AD), shape(color, radius), shape(Color.WHITE, radius)); }
    private void gap(LinearLayout layout, int height) { layout.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void handleBack() { if ("game".equals(screen)) pauseMenu(); else if (!"home".equals(screen)) home(); else finish(); }
    @SuppressWarnings("deprecation") @Override public void onBackPressed() { handleBack(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_SCREEN, screen);
        outState.putString(STATE_GAME, activeGame.name());
        outState.putBoolean(STATE_ACTIVE_RUN, "game".equals(screen) && gameView != null);
        super.onSaveInstanceState(outState);
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        root.requestApplyInsets();
        if (gameView != null) { gameView.setForeground(false); gameView.setForeground(true); }
        else if ("home".equals(screen)) home();
        else if ("rankings".equals(screen)) rankings();
        if (banner != null) banner.reloadForConfiguration();
    }
    @Override protected void onPause() { if (gameView != null) gameView.setForeground(false); if (banner != null) banner.pause(); super.onPause(); }
    @Override protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.setForeground(true);
        if (banner != null) banner.resume();
        if (ranking != null) ranking.syncPending();
    }
    @Override protected void onDestroy() {
        if (banner != null) banner.dispose();
        if (ranking != null) ranking.close();
        super.onDestroy();
    }
}
