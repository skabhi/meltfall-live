package com.anderson.singh.play.meltfalllive;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.view.Surface;
import android.view.SurfaceHolder;

final class GlRainRenderThread extends Thread {
    private final Context context;
    private final Object nativeWindow;
    private final Object lock = new Object();

    private boolean running = true;
    private boolean paused;
    private int width = 1;
    private int height = 1;

    GlRainRenderThread(Context context, SurfaceHolder holder) {
        this(context, (Object) holder);
    }

    GlRainRenderThread(Context context, SurfaceTexture surfaceTexture) {
        this(context, new Surface(surfaceTexture));
    }

    private GlRainRenderThread(Context context, Object nativeWindow) {
        super("MeltfallGlRenderer");
        this.context = context.getApplicationContext();
        this.nativeWindow = nativeWindow;
    }

    void setSurfaceSize(int width, int height) {
        synchronized (lock) {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }
    }

    void setPaused(boolean paused) {
        synchronized (lock) {
            this.paused = paused;
            lock.notifyAll();
        }
    }

    void shutdown() {
        synchronized (lock) {
            running = false;
            lock.notifyAll();
        }
        interrupt();
    }

    @Override
    public void run() {
        EglState egl = null;
        try {
            egl = EglState.create(nativeWindow);
            GlRainRenderer renderer = new GlRainRenderer(context);
            renderer.onSurfaceCreated();
            long lastFrame = 0L;

            while (isRunning()) {
                waitIfPaused();

                int currentWidth;
                int currentHeight;
                synchronized (lock) {
                    currentWidth = width;
                    currentHeight = height;
                }
                renderer.onSurfaceChanged(currentWidth, currentHeight);

                long now = System.nanoTime();
                float elapsed = lastFrame == 0L ? 0f : (now - lastFrame) / 1_000_000_000f;
                lastFrame = now;
                RainSettings.Values settings = RainSettings.load(context);
                renderer.render(Math.min(0.05f, elapsed), settings);
                egl.swap();

                sleepForFrameLimit(settings.maxFps, now);
            }
        } finally {
            if (egl != null) {
                egl.release();
            }
            if (nativeWindow instanceof Surface) {
                ((Surface) nativeWindow).release();
            }
        }
    }

    private boolean isRunning() {
        synchronized (lock) {
            return running;
        }
    }

    private void waitIfPaused() {
        synchronized (lock) {
            while (running && paused) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void sleepForFrameLimit(int maxFps, long frameStartNanos) {
        long targetNanos = maxFps <= 0 ? 8_333_333L : 1_000_000_000L / maxFps;
        long elapsed = System.nanoTime() - frameStartNanos;
        long remainingMillis = (targetNanos - elapsed) / 1_000_000L;
        if (remainingMillis > 0L) {
            try {
                Thread.sleep(remainingMillis);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static final class EglState {
        private final EGLDisplay display;
        private final EGLContext context;
        private final EGLSurface surface;

        private EglState(EGLDisplay display, EGLContext context, EGLSurface surface) {
            this.display = display;
            this.context = context;
            this.surface = surface;
        }

        static EglState create(Object nativeWindow) {
            EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            EGL14.eglInitialize(display, version, 0, version, 1);

            int[] configAttributes = {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] configCount = new int[1];
            EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, 1, configCount, 0);

            int[] contextAttributes = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            EGLContext context = EGL14.eglCreateContext(display, configs[0],
                    EGL14.EGL_NO_CONTEXT, contextAttributes, 0);

            int[] surfaceAttributes = {EGL14.EGL_NONE};
            EGLSurface surface = EGL14.eglCreateWindowSurface(display, configs[0],
                    nativeWindow, surfaceAttributes, 0);
            EGL14.eglMakeCurrent(display, surface, surface, context);
            return new EglState(display, context, surface);
        }

        void swap() {
            EGL14.eglSwapBuffers(display, surface);
        }

        void release() {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(display, surface);
            EGL14.eglDestroyContext(display, context);
            EGL14.eglTerminate(display);
        }
    }
}
