package com.anderson.singh.play.meltfalllive;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

public final class RainSettings {
    private static final String PREFS = "funky_face_rain_settings";
    private static final String SPEED = "speed_percent";
    private static final String EMOJI_COUNT = "emoji_count";
    private static final String SIZE = "size_percent";
    private static final String FPS = "fps";
    private static final String SHOW_FPS = "show_fps";

    public static final int DEFAULT_SPEED = 100;
    public static final int DEFAULT_EMOJI_COUNT = 240;
    public static final int DEFAULT_SIZE = 100;
    public static final int DEFAULT_FPS = 0;
    public static final boolean DEFAULT_SHOW_FPS = false;

    private RainSettings() {
    }

    public static Values load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new Values(
                prefs.getInt(SPEED, DEFAULT_SPEED),
                prefs.getInt(EMOJI_COUNT, DEFAULT_EMOJI_COUNT),
                prefs.getInt(SIZE, DEFAULT_SIZE),
                prefs.getInt(FPS, DEFAULT_FPS),
                prefs.getBoolean(SHOW_FPS, DEFAULT_SHOW_FPS)
        );
    }

    public static void save(Context context, Values values) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(SPEED, clamp(values.speedPercent, 25, 200))
                .putInt(EMOJI_COUNT, values.emojiCount)
                .putInt(SIZE, clamp(values.sizePercent, 60, 170))
                .putInt(FPS, values.maxFps)
                .putBoolean(SHOW_FPS, values.showFps)
                .apply();
    }

    public static void reset(Context context) {
        save(context, new Values(DEFAULT_SPEED, DEFAULT_EMOJI_COUNT, DEFAULT_SIZE, DEFAULT_FPS));
    }

    public static boolean canShowFpsCounter(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static boolean shouldShowFpsCounter(Context context) {
        return canShowFpsCounter(context) && load(context).showFps;
    }

    public static final class Values {
        public final int speedPercent;
        public final int emojiCount;
        public final int sizePercent;
        public final int maxFps;
        public final boolean showFps;

        public Values(int speedPercent, int emojiCount, int sizePercent, int maxFps) {
            this(speedPercent, emojiCount, sizePercent, maxFps, DEFAULT_SHOW_FPS);
        }

        public Values(int speedPercent, int emojiCount, int sizePercent, int maxFps, boolean showFps) {
            this.speedPercent = clamp(speedPercent, 25, 200);
            this.emojiCount = Math.max(0, emojiCount);
            this.sizePercent = clamp(sizePercent, 60, 170);
            this.maxFps = normalizeFps(maxFps);
            this.showFps = showFps;
        }

        public float speedScale() {
            return speedPercent / 100f;
        }

        public float sizeScale() {
            return sizePercent / 100f;
        }
    }

    private static int normalizeFps(int fps) {
        if (fps == 30 || fps == 45 || fps == 60) {
            return fps;
        }
        return DEFAULT_FPS;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
