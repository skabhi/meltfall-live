package com.anderson.singh.play.meltfalllive;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

public class RainView extends TextureView implements TextureView.SurfaceTextureListener {
    private GlRainRenderThread renderThread;

    public RainView(Context context) {
        super(context);
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        renderThread = new GlRainRenderThread(getContext(), surface);
        renderThread.setSurfaceSize(width, height);
        renderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (renderThread != null) {
            renderThread.setSurfaceSize(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (renderThread != null) {
            renderThread.shutdown();
            try {
                renderThread.join(500L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            renderThread = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
