package com.yamone.arcade2;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.yamone.arcade2.core.GameId;
import com.yamone.arcade2.core.ColorBreakEngine;
import com.yamone.arcade2.core.OrbitEngine;
import com.yamone.arcade2.core.TwinTapEngine;
import com.yamone.arcade2.data.LocalStore;
import com.yamone.arcade2.ui.ColorBreakView;
import com.yamone.arcade2.ui.OrbitView;
import com.yamone.arcade2.ui.TwinTapView;
import com.yamone.arcade2.data.RankingGateway;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/** Offline, emulator-only fixtures. Not packaged in the user APK or sent to the server. */
public final class UiSmokeInstrumentation extends Instrumentation {
    private Activity activity;
    private String scenario;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); scenario = arguments.getString("scenario", "standard"); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            activity = startActivitySync(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            int widthDp = activity.getResources().getConfiguration().screenWidthDp;
            float fontScale = activity.getResources().getConfiguration().fontScale;
            if ("standard".equals(scenario) && widthDp < 360) throw new AssertionError("Standard device too narrow: " + widthDp);
            if ("large-text".equals(scenario) && (widthDp > 360 || fontScale < 1.25f)) throw new AssertionError("Compact large-text scenario not applied: " + widthDp + "dp, font " + fontScale);
            requireText("오늘도, 가볍게 한 판 ✦"); capture("home");
            runOnMainSync(() -> scroll(activity.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN));
            capture("home-games");
            navigate("설정"); requireText("나의 작은 취향"); capture("settings");
            runOnMainSync(() -> scroll(activity.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN)); capture("settings-records");
            navigate("랭킹"); requireText("반짝이는 기록들");
            // Preserve the live client's explicit offline state in its own screenshot.
            render(RankingGateway.Status.OFFLINE, null); capture("rankings-offline");
            render(RankingGateway.Status.SUCCESS, new RankingGateway.Board(GameId.ORBIT_SNAP, 0, Collections.emptyList(), null, Collections.emptyList()));
            requireText("첫 번째 별이 되어주세요"); capture("rankings-empty");
            RankingGateway.Entry mine = new RankingGateway.Entry(3, 8750, "나의작은별", "KR", true);
            List<RankingGateway.Entry> top = Arrays.asList(new RankingGateway.Entry(1, 12450, "달빛토끼", "KR", false),
                new RankingGateway.Entry(2, 10200, "Peach", "CA", false), mine,
                new RankingGateway.Entry(4, 7400, "민트구름", "KR", false));
            render(RankingGateway.Status.SUCCESS, new RankingGateway.Board(GameId.ORBIT_SNAP, 4, top, mine, top));
            capture("rankings-fixture");
            runOnMainSync(() -> scroll(activity.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN)); capture("rankings-list-fixture");
            navigate("홈"); requireText("오늘도, 가볍게 한 판 ✦");
            colorBreak();
            orbit();
            tapTap();
            result.putString("stream", "UI_SMOKE_OK: " + scenario + " " + widthDp + "dp, font " + fontScale + " menus, Color Break, Orbit and four-lane Tap Tap input/multitouch/pause/results/records\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "UI_SMOKE_FAILED: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void colorBreak() throws Exception {
        LocalStore local = new LocalStore(getTargetContext());
        int previousPlays = local.plays(GameId.COLOR_BREAK);
        String playerId = local.playerId();
        navigate("컬러 브레이크");
        ColorBreakView board = activity.getWindow().getDecorView().findViewWithTag("colorBreakBoard");
        if (board == null) throw new AssertionError("Color Break should open directly without a start dialog");
        java.lang.reflect.Field field = ColorBreakView.class.getDeclaredField("engine"); field.setAccessible(true);
        ColorBreakEngine engine = (ColorBreakEngine)field.get(board);
        if (engine.state() != ColorBreakEngine.State.READY) throw new AssertionError("Board must wait for pad input");
        capture("color-ready");
        runOnMainSync(() -> {
            tap(board, board.getWidth()/2f, 20);
            if (engine.state() != ColorBreakEngine.State.READY) throw new AssertionError("Score area must not start play");
            for (int lane = 0; lane < 4; lane++) {
                pad(board, lane);
                if (engine.lane() != lane) throw new AssertionError("Pad mapping failed: " + lane);
            }
        });
        navigate("일시정지"); requireText("잠깐, 숨 고르기");
        double pausedAt = engine.elapsed(); capture("color-pause");
        if (engine.elapsed() != pausedAt) throw new AssertionError("Pause panel did not freeze clock");
        navigate("계속 플레이");
        runOnMainSync(() -> {
            for (int wall = 0; wall < 36; wall++) {
                int match = 0; while (engine.laneColor(match) != engine.targetColor()) match++;
                pad(board, match);
                engine.advance(engine.wallDuration() * (1-engine.wallProgress())); engine.advance(ColorBreakEngine.RECOVERY);
            }
            if (engine.score() <= 0 || engine.passed() != 36) throw new AssertionError("Pad play did not award 36 walls");
            pad(board, 2); engine.advance(engine.wallDuration() * .15); board.invalidate();
        });
        capture("color-playing");
        runOnMainSync(() -> { engine.advance(1000); board.invalidate(); });
        waitForIdleSync(); Thread.sleep(250); waitForIdleSync();
        requireText("다섯 번의 미스, 여기까지 잘 달렸어요."); capture("color-result");
        if (local.plays(GameId.COLOR_BREAK) != previousPlays + 1 || local.best(GameId.COLOR_BREAK) < engine.score())
            throw new AssertionError("Terminal score/play count not saved exactly once");
        if (!local.playerId().equals(playerId)) throw new AssertionError("Player identity changed");
        navigate("한 판 더  →");
        ColorBreakView fresh = activity.getWindow().getDecorView().findViewWithTag("colorBreakBoard");
        ColorBreakEngine freshEngine = (ColorBreakEngine)field.get(fresh);
        if (freshEngine.state() != ColorBreakEngine.State.READY || freshEngine.lives() != 5 || freshEngine.score() != 0)
            throw new AssertionError("Retry must create a clean five-life game");
        navigate("일시정지"); navigate("홈으로");
        if (local.plays(GameId.COLOR_BREAK) != previousPlays + 1) throw new AssertionError("Abandoned retry saved a fake result");
    }
    private void orbit() throws Exception {
        LocalStore local = new LocalStore(getTargetContext());
        int previousPlays = local.plays(GameId.ORBIT_SNAP);
        String playerId = local.playerId();
        navigate("지금 플레이  →");
        OrbitView board = activity.getWindow().getDecorView().findViewWithTag("orbitBoard");
        if (board == null || board.engine().state() != OrbitEngine.State.RUNNING)
            throw new AssertionError("Orbit should rotate immediately without an entry dialog");
        OrbitEngine engine = board.engine();
        double angle = engine.angle();
        capture("orbit-playing");
        if (engine.angle() == angle) throw new AssertionError("Orbit did not rotate without holding");
        navigate("일시정지"); requireText("빙글빙글, 궤도도 잠시 쉬어요.\n준비되면 타이밍을 이어가요!");
        double pausedAt = engine.elapsed(); angle = engine.angle(); int pausedLives = engine.lives();
        capture("orbit-pause");
        if (engine.elapsed() != pausedAt || engine.angle() != angle) throw new AssertionError("Orbit pause did not freeze rotation");
        runOnMainSync(() -> tap(board, board.getWidth()/2f, board.getHeight()-100));
        if (engine.lives() != pausedLives) throw new AssertionError("Paused board accepted input");
        navigate("계속 플레이");
        onMain(() -> {
            int livesBefore = engine.lives();
            tap(board, board.getWidth()/2f, 20);
            if (engine.lives() != livesBefore || engine.score() != 0) throw new AssertionError("Score area incorrectly judges a tap");
            // Reset the View's real-frame baseline before driving controlled engine time.
            board.pauseGame(); board.resumeGame();
            for (int i=0; i<20; i++) {
                // A screenshot may leave the dot past the center but inside the window.
                // Tap that valid window now instead of advancing a whole extra revolution.
                if (!engine.inTarget()) engine.advance(((engine.target()-engine.angle()+360)%360) / engine.speed());
                float scale = Math.min(board.getWidth()/360f, board.getHeight()/500f);
                tap(board, board.getWidth()/2f, board.getHeight()-42*scale);
            }
            if (engine.jumps() != 20 || engine.lives() != livesBefore || engine.score() <= 0)
                throw new AssertionError("Orbit actual pad input did not award twenty target hits");
            long now = SystemClock.uptimeMillis();
            MotionEvent move = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, board.getWidth()/2f, board.getHeight()-100, 0);
            MotionEvent release = MotionEvent.obtain(now, now+1, MotionEvent.ACTION_UP, board.getWidth()/2f, board.getHeight()-100, 0);
            try { board.dispatchTouchEvent(move); board.dispatchTouchEvent(release); }
            finally { move.recycle(); release.recycle(); }
            if (engine.jumps() != 20 || engine.lives() != livesBefore) throw new AssertionError("Move/release incorrectly judged a target");
            board.invalidate();
        });
        capture("orbit-combo");
        runOnMainSync(() -> { callActivityOnPause(activity); callActivityOnResume(activity); });
        waitForIdleSync();
        if (!board.isPaused() || activity.getWindow().getDecorView().findViewWithTag("orbitPause") == null)
            throw new AssertionError("Orbit foreground recovery requires explicit resume");
        navigate("계속 플레이");
        runOnMainSync(() -> { engine.advance(1000); board.invalidate(); });
        waitForIdleSync(); Thread.sleep(250); waitForIdleSync();
        requireText("다섯 번의 미스, 여기까지 잘 달렸어요."); capture("orbit-result");
        if (activity.getWindow().getDecorView().findViewWithTag("orbitResult") == null)
            throw new AssertionError("Orbit custom result sheet missing");
        if (local.plays(GameId.ORBIT_SNAP) != previousPlays+1 || local.best(GameId.ORBIT_SNAP) < engine.score())
            throw new AssertionError("Orbit terminal record must save exactly once");
        if (!local.playerId().equals(playerId)) throw new AssertionError("Orbit changed installation identity");
        navigate("한 판 더  →");
        OrbitView fresh = activity.getWindow().getDecorView().findViewWithTag("orbitBoard");
        if (fresh.engine().state() != OrbitEngine.State.RUNNING || fresh.engine().lives() != 5 || fresh.engine().score() != 0)
            throw new AssertionError("Orbit retry must reset score/lives and rotate immediately");
        navigate("일시정지"); navigate("홈으로");
        if (local.plays(GameId.ORBIT_SNAP) != previousPlays+1) throw new AssertionError("Orbit abandoned retry saved a false result");
    }
    private void onMain(Runnable task) {
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> { try { task.run(); } catch (Throwable error) { failure[0] = error; } });
        if (failure[0] != null) throw new AssertionError("Main-thread UI assertion", failure[0]);
    }
    private void tapTap() throws Exception {
        LocalStore local=new LocalStore(getTargetContext());
        int previousPlays=local.plays(GameId.TWIN_TAP); String playerId=local.playerId();
        navigate("탭탭");
        TwinTapView board=activity.getWindow().getDecorView().findViewWithTag("tapTapBoard");
        if (board==null || board.engine().state()!=TwinTapEngine.State.READY) throw new AssertionError("Tap Tap should enter without a dialog");
        TwinTapEngine engine=board.engine(); capture("tap-tap-ready");
        onMain(()-> {
            tap(board,board.getWidth()/2f,20);
            tap(board,board.getWidth()/2f,board.getHeight()/2f);
            if (engine.state()!=TwinTapEngine.State.READY) throw new AssertionError("HUD/runway started Tap Tap");
            tapTapPad(board,0);
            if (engine.state()!=TwinTapEngine.State.RUNNING || engine.score()!=0) throw new AssertionError("First pad should only start rhythm");
        });
        navigate("일시정지"); requireText("반짝이는 노트도 잠깐 쉬어요.\n준비되면 톡톡, 이어가요!");
        double pausedAt=engine.elapsed(); int pausedLives=engine.lives(); capture("tap-tap-pause");
        onMain(()->tapTapPad(board,0));
        if (engine.elapsed()!=pausedAt || engine.lives()!=pausedLives) throw new AssertionError("Tap Tap pause accepted input/time");
        navigate("계속 플레이");
        onMain(()-> {
            board.pauseGame(); board.resumeGame();
            int lanes=0, rows=0, priorLives=engine.lives();
            while (rows<24 || lanes!=15 || engine.simultaneousHits()==0) {
                if (rows>=256) throw new AssertionError("Missing four-lane/chord coverage");
                if (engine.targetOffset()>0) engine.advance(engine.targetOffset());
                int mask=engine.noteMask(); lanes|=mask;
                nativeNote(board,mask); rows++;
                if (engine.hits()!=rows || engine.lives()!=priorLives) throw new AssertionError("Native note/chord did not score once");
            }
            engine.advance(engine.travelTime()*.35); board.invalidate();
        });
        capture("tap-tap-playing");
        onMain(()-> { callActivityOnPause(activity); callActivityOnResume(activity); }); waitForIdleSync();
        if (!board.isPaused() || activity.getWindow().getDecorView().findViewWithTag("tapTapPause")==null)
            throw new AssertionError("Tap Tap foreground needs explicit resume");
        navigate("계속 플레이");
        onMain(()-> { engine.advance(1000); board.invalidate(); });
        waitForIdleSync(); Thread.sleep(250); waitForIdleSync();
        requireText("다섯 번의 미스, 여기까지 잘 달렸어요."); capture("tap-tap-result");
        if (local.plays(GameId.TWIN_TAP)!=previousPlays+1 || local.best(GameId.TWIN_TAP)<engine.score() || !local.playerId().equals(playerId))
            throw new AssertionError("Tap Tap records/identity changed unexpectedly");
        onMain(()->scroll(activity.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN)); capture("tap-tap-result-actions");
        navigate("한 판 더  →");
        TwinTapView fresh=activity.getWindow().getDecorView().findViewWithTag("tapTapBoard");
        if (fresh.engine().state()!=TwinTapEngine.State.READY || fresh.engine().lives()!=5 || fresh.engine().score()!=0)
            throw new AssertionError("Tap Tap retry did not reset");
        navigate("일시정지"); navigate("홈으로");
        if (local.plays(GameId.TWIN_TAP)!=previousPlays+1) throw new AssertionError("Tap Tap abandoned run saved a result");
    }
    private float tapTapX(TwinTapView board,int lane) {
        float scale=Math.min(board.getWidth()/360f,board.getHeight()/520f);
        return (board.getWidth()-360*scale)/2+(58.5f+81*lane)*scale;
    }
    private float tapTapY(TwinTapView board) { return board.getHeight()-43*Math.min(board.getWidth()/360f,board.getHeight()/520f); }
    private void tapTapPad(TwinTapView board,int lane) { tap(board,tapTapX(board,lane),tapTapY(board)); }
    private void nativeNote(TwinTapView board,int mask) {
        int first=Integer.numberOfTrailingZeros(mask);
        if (Integer.bitCount(mask)==1) { tapTapPad(board,first); return; }
        int second=Integer.numberOfTrailingZeros(mask&~(1<<first));
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[2];
        for (int i=0;i<2;i++) {
            properties[i]=new MotionEvent.PointerProperties(); properties[i].id=i==0?7:11; properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords(); coords[i].x=tapTapX(board,i==0?first:second); coords[i].y=tapTapY(board); coords[i].pressure=1; coords[i].size=1;
        }
        long now=SystemClock.uptimeMillis();
        sendPointers(board,now,MotionEvent.ACTION_DOWN,1,properties,coords);
        int partial=board.engine().tappedMask();
        sendPointers(board,now,MotionEvent.ACTION_MOVE,1,properties,coords);
        sendPointers(board,now,MotionEvent.ACTION_DOWN,1,properties,coords);
        if (partial!=(1<<first) || board.engine().tappedMask()!=partial) throw new AssertionError("Hold/repeated pointer judged a second lane");
        sendPointers(board,now,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,properties,coords);
        sendPointers(board,now,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,properties,coords);
        sendPointers(board,now,MotionEvent.ACTION_UP,1,properties,coords);
    }
    private void sendPointers(View board,long time,int action,int count,MotionEvent.PointerProperties[] properties,MotionEvent.PointerCoords[] coords) {
        MotionEvent event=MotionEvent.obtain(time,time,action,count,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        try { board.dispatchTouchEvent(event); } finally { event.recycle(); }
    }
    private void pad(ColorBreakView board, int lane) {
        float scale = Math.min(board.getWidth()/360f, board.getHeight()/480f);
        float left = (board.getWidth()-360*scale)/2;
        tap(board, left + (57+80*lane)*scale, board.getHeight()-58*scale);
    }
    private void tap(View view, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now+1, MotionEvent.ACTION_UP, x, y, 0);
        try { view.dispatchTouchEvent(down); view.dispatchTouchEvent(up); } finally { down.recycle(); up.recycle(); }
    }
    private void navigate(String label) {
        runOnMainSync(() -> {
            View target = findText(activity.getWindow().getDecorView(), label);
            if (target == null) throw new AssertionError("Missing navigation: " + label);
            while (!target.isClickable() && target.getParent() instanceof View) target = (View) target.getParent();
            if (!target.performClick()) throw new AssertionError("Navigation not clickable: " + label);
        }); waitForIdleSync();
    }
    private void requireText(String text) {
        runOnMainSync(() -> { if (findText(activity.getWindow().getDecorView(), text) == null) throw new AssertionError("Missing: " + text); });
    }
    private View findText(View view, String value) {
        if (view instanceof TextView && value.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = findText(((ViewGroup) view).getChildAt(i), value); if (found != null) return found;
        }
        return null;
    }
    private ScrollView scroll(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            ScrollView found = scroll(((ViewGroup) view).getChildAt(i)); if (found != null) return found;
        }
        return null;
    }
    private void render(RankingGateway.Status status, RankingGateway.Board board) throws Exception {
        // Ensure any offline callback finishes before installing deterministic visual fixtures.
        Thread.sleep(1200);
        Method method = MainActivity.class.getDeclaredMethod("renderOnlineRanking", LinearLayout.class, RankingGateway.Status.class, RankingGateway.Board.class);
        method.setAccessible(true);
        runOnMainSync(() -> {
            try { method.invoke(activity, activity.getWindow().getDecorView().findViewWithTag("onlineRanking"), status, board); }
            catch (Exception e) { throw new AssertionError(e); }
        }); waitForIdleSync();
    }
    private void capture(String name) throws Exception {
        waitForIdleSync(); Thread.sleep(500);
        Bitmap bitmap = getUiAutomation().takeScreenshot(); if (bitmap == null) throw new AssertionError("Screenshot unavailable");
        File directory = new File(getTargetContext().getExternalFilesDir(null), "ui-preview");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new AssertionError("Screenshot folder unavailable");
        try (FileOutputStream output = new FileOutputStream(new File(directory, scenario + "-" + name + ".png"))) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new AssertionError("Screenshot encoding failed");
        } finally { bitmap.recycle(); }
    }
}
