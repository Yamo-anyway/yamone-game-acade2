package com.yamone.arcade2.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.yamone.arcade2.BuildConfig;

/** Isolated banner strip outside all game touch targets. Only official demo ads in debug. */
public final class BannerSlot extends FrameLayout {
    private AdView ad;
    private boolean disposed;
    private final TextView status;
    public BannerSlot(Activity activity) {
        super(activity);
        setBackgroundColor(0xFF10162B);
        int h = Math.round(60 * getResources().getDisplayMetrics().density);
        setMinimumHeight(h);
        status = new TextView(activity); status.setTextSize(10); status.setTextColor(0xFF8D97B7);
        status.setGravity(Gravity.CENTER);
        status.setText(BuildConfig.TEST_BANNER_ENABLED ? "테스트 배너를 불러오는 중" : "배너 광고 영역");
        addView(status, new LayoutParams(LayoutParams.MATCH_PARENT, h));
        if (BuildConfig.TEST_BANNER_ENABLED) {
            new Thread(() -> MobileAds.initialize(activity.getApplicationContext(), ignored ->
                activity.runOnUiThread(() -> post(() -> load(activity)))), "ads-init").start();
        }
    }
    private void load(Activity activity) {
        if (disposed || activity.isFinishing() || activity.isDestroyed() || ad != null) return;
        int width = Math.max(1, Math.round(getWidth() / getResources().getDisplayMetrics().density));
        ad = new AdView(activity);
        ad.setAdUnitId("ca-app-pub-3940256099942544/9214589741");
        ad.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width));
        ad.setBackgroundColor(Color.TRANSPARENT);
        ad.setAdListener(new AdListener() {
            @Override public void onAdLoaded() { status.setVisibility(GONE); }
            @Override public void onAdFailedToLoad(LoadAdError error) { status.setText("광고 연결을 기다리는 중"); }
        });
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        addView(ad, lp); ad.loadAd(new AdRequest.Builder().build());
    }
    public void pause() { if (ad != null) ad.pause(); }
    public void resume() { if (ad != null) ad.resume(); }
    public void dispose() { disposed = true; if (ad != null) { ad.destroy(); ad = null; } }
}
