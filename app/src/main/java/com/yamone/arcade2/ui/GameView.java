package com.yamone.arcade2.ui;

import android.content.Context;
import android.view.View;

/** Shared lifecycle boundary; each game keeps its own engine and gestures. */
public abstract class GameView extends View {
    protected GameView(Context context) { super(context); }
    public abstract void pauseGame();
    public abstract void resumeGame();
    public abstract void setForeground(boolean foreground);
}
