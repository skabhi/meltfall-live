package com.anderson.singh.play.meltfalllive;

import android.content.res.Resources;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Random;

public final class RainScene {
    private static final float SPEED_SCALE = 0.5f;
    private static final float SIZE_SCALE = 1.3f;

    private final Resources resources;
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final ArrayList<Drop> drops = new ArrayList<>();
    private final Bitmap[] emojis;
    private RainSettings.Values settings;

    private int width = 1;
    private int height = 1;

    public RainScene(Context context) {
        this(context.getResources(), RainSettings.load(context));
    }

    public RainScene(Resources resources, RainSettings.Values settings) {
        this.resources = resources;
        this.settings = settings;
        emojis = new Bitmap[] {
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_acid),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_cyan),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_hotred),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_lime),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_orange),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_pink),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_violet),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_yellow)
        };
    }

    public void setSettings(RainSettings.Values settings) {
        boolean countChanged = this.settings.emojiCount != settings.emojiCount;
        this.settings = settings;
        if (countChanged) {
            updateDropCountForArea();
        }
    }

    public void setSize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        updateDropCountForArea();
        for (Drop drop : drops) {
            resetDrop(drop, true);
        }
    }

    public void update(float elapsed) {
        float clampedElapsed = Math.min(0.05f, elapsed);
        for (Drop drop : drops) {
            drop.y += drop.speed * clampedElapsed;
            drop.x += drop.drift * clampedElapsed;
            drop.rotation += drop.spin * clampedElapsed;

            if (drop.y > height + drop.size) {
                resetDrop(drop, false);
            }
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawColor(Color.rgb(4, 8, 6));

        for (Drop drop : drops) {
            if (drop.y < -drop.size) {
                continue;
            }

            paint.setAlpha((int) (drop.alpha * 255));
            float scaleX = drop.size / drop.emoji.getWidth();
            float scaleY = drop.size / drop.emoji.getHeight();
            matrix.reset();
            matrix.postTranslate(-drop.emoji.getWidth() / 2f, -drop.emoji.getHeight() / 2f);
            matrix.postScale(scaleX, scaleY);
            matrix.postRotate(drop.rotation);
            matrix.postTranslate(drop.x + drop.size / 2f, drop.y + drop.size / 2f);
            canvas.drawBitmap(drop.emoji, matrix, paint);
        }
    }

    public int getDropCount() {
        return drops.size();
    }

    private void resetDrop(Drop drop, boolean initial) {
        float z = random.nextFloat();
        float depth = (float) Math.pow(z, 1.35);
        float depthScale = 0.23f + depth * 2.17f;
        float density = resources.getDisplayMetrics().density;

        float sizeScale = SIZE_SCALE * settings.sizeScale();
        float size = ((42 + random.nextInt(44)) * depthScale * density * sizeScale) / 3f;
        size = Math.max(8f, Math.min(size, 90f * density * sizeScale));

        drop.size = size;
        drop.x = randomRange(-size, width);
        drop.y = -(size + randomRange(16f, height * (initial ? 2.2f : 0.7f)));
        drop.speed = randomRange(180f, 460f) * (0.32f + depth * 2.55f) * density * SPEED_SCALE * settings.speedScale();
        drop.drift = randomRange(-34f, 34f) * depth;
        drop.rotation = randomRange(0f, 360f);
        drop.spin = randomRange(-124f, 124f) * depth;
        drop.alpha = 0.16f + depth * 0.84f;
        drop.emoji = emojis[random.nextInt(emojis.length)];
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private void updateDropCountForArea() {
        int targetCount = settings.emojiCount;

        while (drops.size() < targetCount) {
            Drop drop = new Drop();
            drops.add(drop);
            resetDrop(drop, true);
        }

        while (drops.size() > targetCount) {
            drops.remove(drops.size() - 1);
        }
    }

    private static final class Drop {
        float x;
        float y;
        float size;
        float speed;
        float drift;
        float rotation;
        float spin;
        float alpha;
        Bitmap emoji;
    }
}
