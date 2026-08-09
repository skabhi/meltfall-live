package com.anderson.singh.play.meltfalllive;

import android.content.res.Resources;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;
import java.util.Random;

public final class RainScene {
    private static final float SPEED_SCALE = 0.5f;
    private static final float SIZE_SCALE = 1.3f;
    private static final int KIND_EMOJI = 0;
    private static final int KIND_CIRCLE = 1;
    private static final int KIND_DIAMOND = 2;
    private static final int EMOJI_SPRITE_COUNT = 8;
    private static final int CIRCLE_SPRITE_INDEX = 8;
    private static final int DIAMOND_SPRITE_INDEX = 9;

    private final Resources resources;
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final ArrayList<Drop> drops = new ArrayList<>();
    private final Bitmap[] sprites;
    private RainSettings.Values settings;

    private int width = 1;
    private int height = 1;

    public RainScene(Context context) {
        this(context.getResources(), RainSettings.load(context));
    }

    public RainScene(Resources resources, RainSettings.Values settings) {
        this.resources = resources;
        this.settings = settings;
        sprites = new Bitmap[] {
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_acid),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_cyan),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_hotred),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_lime),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_orange),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_pink),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_violet),
                BitmapFactory.decodeResource(resources, R.drawable.melting_face_yellow),
                createCircleSprite(),
                createDiamondSprite()
        };
    }

    public void setSettings(RainSettings.Values settings) {
        boolean countChanged = this.settings.emojiCount != settings.emojiCount
                || this.settings.circleCount != settings.circleCount
                || this.settings.diamondCount != settings.diamondCount;
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
            Bitmap sprite = sprites[drop.spriteIndex];
            float scaleX = drop.size / sprite.getWidth();
            float scaleY = drop.size / sprite.getHeight();
            matrix.reset();
            matrix.postTranslate(-sprite.getWidth() / 2f, -sprite.getHeight() / 2f);
            matrix.postScale(scaleX, scaleY);
            matrix.postRotate(drop.rotation);
            matrix.postTranslate(drop.x + drop.size / 2f, drop.y + drop.size / 2f);
            canvas.drawBitmap(sprite, matrix, paint);
        }
    }

    public int getDropCount() {
        return drops.size();
    }

    public ArrayList<Drop> getDrops() {
        return drops;
    }

    public Bitmap[] getEmojis() {
        return sprites;
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
        drop.alpha = drop.kind == KIND_EMOJI ? 0.16f + depth * 0.84f : 0.28f + depth * 0.72f;
        drop.spriteIndex = spriteIndexForKind(drop.kind);
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private void updateDropCountForArea() {
        updateKindCount(KIND_EMOJI, settings.emojiCount);
        updateKindCount(KIND_CIRCLE, settings.circleCount);
        updateKindCount(KIND_DIAMOND, settings.diamondCount);
    }

    private void updateKindCount(int kind, int targetCount) {
        int currentCount = countKind(kind);
        while (currentCount < targetCount) {
            Drop drop = new Drop();
            drop.kind = kind;
            drops.add(drop);
            resetDrop(drop, true);
            currentCount++;
        }

        while (currentCount > targetCount) {
            int index = lastIndexOfKind(kind);
            if (index < 0) {
                return;
            }
            drops.remove(index);
            currentCount--;
        }
    }

    private int countKind(int kind) {
        int count = 0;
        for (Drop drop : drops) {
            if (drop.kind == kind) {
                count++;
            }
        }
        return count;
    }

    private int lastIndexOfKind(int kind) {
        for (int i = drops.size() - 1; i >= 0; i--) {
            if (drops.get(i).kind == kind) {
                return i;
            }
        }
        return -1;
    }

    private int spriteIndexForKind(int kind) {
        if (kind == KIND_CIRCLE) {
            return CIRCLE_SPRITE_INDEX;
        }
        if (kind == KIND_DIAMOND) {
            return DIAMOND_SPRITE_INDEX;
        }
        return random.nextInt(EMOJI_SPRITE_COUNT);
    }

    private static Bitmap createCircleSprite() {
        int size = 256;
        float center = size / 2f;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(center, center, center * 0.82f,
                new int[] {
                        Color.argb(235, 120, 255, 225),
                        Color.argb(205, 36, 220, 170),
                        Color.argb(0, 36, 220, 170)
                },
                new float[] {0f, 0.62f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(center, center, center * 0.78f, paint);
        return bitmap;
    }

    private static Bitmap createDiamondSprite() {
        int size = 256;
        float center = size / 2f;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(220, 255, 80, 220));
        Path path = new Path();
        path.moveTo(center, size * 0.08f);
        path.lineTo(size * 0.92f, center);
        path.lineTo(center, size * 0.92f);
        path.lineTo(size * 0.08f, center);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawCircle(center, center, size * 0.16f, paint);
        return bitmap;
    }

    static final class Drop {
        float x;
        float y;
        float size;
        float speed;
        float drift;
        float rotation;
        float spin;
        float alpha;
        int kind;
        int spriteIndex;
    }
}
