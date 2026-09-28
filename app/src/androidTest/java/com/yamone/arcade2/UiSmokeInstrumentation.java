package com.yamone.arcade2;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.yamone.arcade2.core.GameId;
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
            result.putString("stream", "UI_SMOKE_OK: " + scenario + " home/settings/rankings offline/empty/populated/navigation\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "UI_SMOKE_FAILED: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
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
