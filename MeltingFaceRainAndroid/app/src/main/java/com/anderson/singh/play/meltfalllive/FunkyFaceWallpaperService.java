package com.anderson.singh.play.meltfalllive;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceHolder;

public class FunkyFaceWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new RainEngine();
    }

    private final class RainEngine extends Engine implements Choreographer.FrameCallback {
        private final RainScene scene = new RainScene(FunkyFaceWallpaperService.this);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean visible;
        private long lastFrameNanos;
        private long renderedFpsStartNanos;
        private int renderedFrames;
        private int renderedFps;

        private RainEngine() {
            textPaint.setColor(Color.rgb(220, 255, 225));
            textPaint.setTextSize(42f);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setShadowLayer(6f, 0f, 2f, Color.BLACK);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            scene.setSize(width, height);
            drawFrame();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                lastFrameNanos = 0L;
                scheduleNextFrame(RainSettings.load(FunkyFaceWallpaperService.this));
            } else {
                Choreographer.getInstance().removeFrameCallback(this);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            Choreographer.getInstance().removeFrameCallback(this);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            if (!visible) {
                return;
            }

            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameTimeNanos;
                drawFrame();
                scheduleNextFrame(RainSettings.load(FunkyFaceWallpaperService.this));
                return;
            }

            RainSettings.Values settings = RainSettings.load(FunkyFaceWallpaperService.this);
            float elapsed = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f;
            lastFrameNanos = frameTimeNanos;
            scene.setSettings(settings);
            scene.update(elapsed);
            drawFrame(settings);
            scheduleNextFrame(settings);
        }

        private void drawFrame() {
            drawFrame(RainSettings.load(FunkyFaceWallpaperService.this));
        }

        private void drawFrame(RainSettings.Values settings) {
            SurfaceHolder holder = getSurfaceHolder();
            Surface surface = holder.getSurface();
            Canvas canvas = null;
            boolean hardwareCanvas = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && surface != null && surface.isValid()) {
                    try {
                        canvas = surface.lockHardwareCanvas();
                        hardwareCanvas = true;
                    } catch (IllegalArgumentException ignored) {
                        canvas = holder.lockCanvas();
                    }
                } else {
                    canvas = holder.lockCanvas();
                }

                if (canvas != null) {
                    scene.draw(canvas);
                    if (RainSettings.shouldShowFpsCounter(FunkyFaceWallpaperService.this)) {
                        canvas.drawText(
                                "FPS: " + renderedFps + "   Drops: " + scene.getDropCount(),
                                canvas.getWidth() - 24f,
                                statusBarSafeTop() + 46f,
                                textPaint
                        );
                    }
                }
            } finally {
                if (canvas != null) {
                    if (hardwareCanvas && surface != null) {
                        surface.unlockCanvasAndPost(canvas);
                    } else {
                        holder.unlockCanvasAndPost(canvas);
                    }
                    recordRenderedFrame();
                }
            }
        }

        private long targetFrameNanos(int maxFps) {
            if (maxFps <= 0) {
                return 0L;
            }
            return 1_000_000_000L / maxFps;
        }

        private void scheduleNextFrame(RainSettings.Values settings) {
            long targetFrameNanos = targetFrameNanos(settings.maxFps);
            if (targetFrameNanos <= 0L) {
                Choreographer.getInstance().postFrameCallback(this);
                return;
            }

            long delayMillis = Math.max(1L, targetFrameNanos / 1_000_000L);
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
}
