package com.anderson.singh.play.meltfalllive;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class FunkyFaceWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new RainEngine();
    }

    private final class RainEngine extends Engine {
        private GlRainRenderThread renderThread;

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            renderThread = new GlRainRenderThread(FunkyFaceWallpaperService.this, holder);
            renderThread.start();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (renderThread != null) {
                renderThread.setSurfaceSize(width, height);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (renderThread != null) {
                renderThread.setPaused(!visible);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            stopRenderer();
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            stopRenderer();
            super.onDestroy();
        }

        private void stopRenderer() {
            if (renderThread != null) {
                renderThread.shutdown();
                try {
                    renderThread.join(500L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                renderThread = null;
            }
        }
    }
}
