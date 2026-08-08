package com.anderson.singh.play.meltfalllive;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Choreographer;
import android.view.View;

public class RainView extends View implements Choreographer.FrameCallback {
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RainScene scene;

    private long lastFrameNanos;
    private long renderedFpsStartNanos;
    private int renderedFrames;
    private int renderedFps;

    public RainView(Context context) {
        super(context);
        scene = new RainScene(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setBackgroundColor(Color.rgb(4, 8, 6));

        textPaint.setColor(Color.rgb(220, 255, 225));
        textPaint.setTextSize(42f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setShadowLayer(6f, 0f, 2f, Color.BLACK);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        scheduleNextFrame(RainSettings.load(getContext()));
    }

    @Override
    protected void onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        scene.setSize(w, h);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = frameTimeNanos;
            scheduleNextFrame(RainSettings.load(getContext()));
            return;
        }

        RainSettings.Values settings = RainSettings.load(getContext());
        float elapsed = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = frameTimeNanos;

        scene.setSettings(settings);
        scene.update(elapsed);

        invalidate();
        scheduleNextFrame(settings);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        scene.draw(canvas);

        if (RainSettings.shouldShowFpsCounter(getContext())) {
            canvas.drawText(
                    "FPS: " + renderedFps + "   Drops: " + scene.getDropCount(),
                    canvas.getWidth() - 24f,
                    statusBarSafeTop() + 46f,
                    textPaint
            );
        }
        recordRenderedFrame();
    }

    private void scheduleNextFrame(RainSettings.Values settings) {
        if (settings.maxFps <= 0) {
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }

        long delayMillis = Math.max(1L, (1_000_000_000L / settings.maxFps) / 1_000_000L);
        Choreographer.getInstance().postFrameCallbackDelayed(this, delayMillis);
    }

    private float statusBarSafeTop() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 24f * getResources().getDisplayMetrics().density;
    }

    private void recordRenderedFrame() {
        long now = System.nanoTime();
        if (renderedFpsStartNanos == 0L) {
            renderedFpsStartNanos = now;
            renderedFrames = 0;
            return;
        }

        renderedFrames++;
        long elapsed = now - renderedFpsStartNanos;
        if (elapsed >= 1_000_000_000L) {
            renderedFps = Math.round(renderedFrames * 1_000_000_000f / elapsed);
            renderedFrames = 0;
            renderedFpsStartNanos = now;
        }
    }
}
