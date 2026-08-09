package com.anderson.singh.play.meltfalllive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

final class GlRainRenderer {
    private static final int FLOATS_PER_QUAD_VERTEX = 4;
    private static final int FLOATS_PER_INSTANCE = 12;
    private static final int QUAD_VERTICES = 6;
    private static final int ATLAS_COLUMNS = 4;
    private static final int ATLAS_ROWS = 2;
    private static final int ALPHA_THRESHOLD = 8;

    private final Context context;
    private final RainScene scene;

    private int program;
    private int textureId;
    private int cornerHandle;
    private int uvCornerHandle;
    private int centerDrawSizeHandle;
    private int alphaRotationHandle;
    private int uvRectHandle;
    private int resolutionHandle;
    private int textureHandle;
    private int width;
    private int height;
    private AtlasEntry[] atlasEntries;
    private final FloatBuffer quadBuffer = allocateQuadBuffer();
    private FloatBuffer instanceBuffer = allocateBuffer(256);

    GlRainRenderer(Context context) {
        this.context = context.getApplicationContext();
        this.scene = new RainScene(context);
    }

    void onSurfaceCreated() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        cornerHandle = GLES20.glGetAttribLocation(program, "aCorner");
        uvCornerHandle = GLES20.glGetAttribLocation(program, "aUvCorner");
        centerDrawSizeHandle = GLES20.glGetAttribLocation(program, "aCenterDrawSize");
        alphaRotationHandle = GLES20.glGetAttribLocation(program, "aAlphaRotation");
        uvRectHandle = GLES20.glGetAttribLocation(program, "aUvRect");
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        Atlas atlas = createAtlasTexture(scene.getEmojis());
        textureId = atlas.textureId;
        atlasEntries = atlas.entries;

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(4f / 255f, 8f / 255f, 6f / 255f, 1f);
    }

    void onSurfaceChanged(int width, int height) {
        int newWidth = Math.max(1, width);
        int newHeight = Math.max(1, height);
        if (this.width == newWidth && this.height == newHeight) {
            return;
        }

        this.width = newWidth;
        this.height = newHeight;
        scene.setSize(this.width, this.height);
        GLES20.glViewport(0, 0, this.width, this.height);
    }

    void render(float elapsedSeconds, RainSettings.Values settings) {
        scene.setSettings(settings);
        scene.update(elapsedSeconds);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        int vertexCount = writeVertices();
        if (vertexCount == 0) {
            return;
        }

        GLES20.glUseProgram(program);
        GLES20.glUniform2f(resolutionHandle, width, height);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        quadBuffer.position(0);
        GLES20.glVertexAttribPointer(cornerHandle, 2, GLES20.GL_FLOAT, false,
                FLOATS_PER_QUAD_VERTEX * 4, quadBuffer);
        GLES20.glEnableVertexAttribArray(cornerHandle);

        quadBuffer.position(2);
        GLES20.glVertexAttribPointer(uvCornerHandle, 2, GLES20.GL_FLOAT, false,
                FLOATS_PER_QUAD_VERTEX * 4, quadBuffer);
        GLES20.glEnableVertexAttribArray(uvCornerHandle);

        instanceBuffer.position(0);
        GLES20.glVertexAttribPointer(centerDrawSizeHandle, 4, GLES20.GL_FLOAT, false,
                FLOATS_PER_INSTANCE * 4, instanceBuffer);
        GLES20.glEnableVertexAttribArray(centerDrawSizeHandle);
        GLES30.glVertexAttribDivisor(centerDrawSizeHandle, 1);

        instanceBuffer.position(4);
        GLES20.glVertexAttribPointer(alphaRotationHandle, 4, GLES20.GL_FLOAT, false,
                FLOATS_PER_INSTANCE * 4, instanceBuffer);
        GLES20.glEnableVertexAttribArray(alphaRotationHandle);
        GLES30.glVertexAttribDivisor(alphaRotationHandle, 1);

        instanceBuffer.position(8);
        GLES20.glVertexAttribPointer(uvRectHandle, 4, GLES20.GL_FLOAT, false,
                FLOATS_PER_INSTANCE * 4, instanceBuffer);
        GLES20.glEnableVertexAttribArray(uvRectHandle);
        GLES30.glVertexAttribDivisor(uvRectHandle, 1);

        GLES30.glDrawArraysInstanced(GLES20.GL_TRIANGLES, 0, QUAD_VERTICES, vertexCount);
    }

    int getDropCount() {
        return scene.getDropCount();
    }

    private int writeVertices() {
        int visibleDrops = 0;
        for (RainScene.Drop drop : scene.getDrops()) {
            if (isVisible(drop)) {
                visibleDrops++;
            }
        }

        int requiredFloats = Math.max(1, visibleDrops * FLOATS_PER_INSTANCE);
        if (instanceBuffer.capacity() < requiredFloats) {
            instanceBuffer = allocateBuffer(requiredFloats);
        }

        instanceBuffer.clear();
        int instanceCount = 0;
        for (RainScene.Drop drop : scene.getDrops()) {
            if (!isVisible(drop)) {
                continue;
            }
            addDropInstance(drop);
            instanceCount++;
        }
        instanceBuffer.position(0);
        return instanceCount;
    }

    private void addDropInstance(RainScene.Drop drop) {
        float half = drop.size / 2f;
        float centerX = drop.x + half;
        float centerY = drop.y + half;
        double radians = Math.toRadians(drop.rotation);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        AtlasEntry entry = atlasEntries[drop.emojiIndex];

        float scaleX = drop.size / entry.sourceWidth;
        float scaleY = drop.size / entry.sourceHeight;
        float drawWidth = entry.width * scaleX;
        float drawHeight = entry.height * scaleY;
        float offsetX = (entry.left + entry.width / 2f - entry.sourceWidth / 2f) * scaleX;
        float offsetY = (entry.top + entry.height / 2f - entry.sourceHeight / 2f) * scaleY;
        float croppedCenterX = centerX + offsetX * cos - offsetY * sin;
        float croppedCenterY = centerY + offsetX * sin + offsetY * cos;

        instanceBuffer.put(croppedCenterX);
        instanceBuffer.put(croppedCenterY);
        instanceBuffer.put(drawWidth);
        instanceBuffer.put(drawHeight);
        instanceBuffer.put(drop.alpha);
        instanceBuffer.put(cos);
        instanceBuffer.put(sin);
        instanceBuffer.put(0f);
        instanceBuffer.put(entry.u);
        instanceBuffer.put(entry.v);
        instanceBuffer.put(entry.uSize);
        instanceBuffer.put(entry.vSize);
    }

    private boolean isVisible(RainScene.Drop drop) {
        float margin = drop.size;
        return drop.x < width + margin
                && drop.x + drop.size > -margin
                && drop.y < height + margin
                && drop.y + drop.size > -margin;
    }

    private Atlas createAtlasTexture(Bitmap[] emojis) {
        AtlasEntry[] entries = new AtlasEntry[emojis.length];
        int cellWidth = 0;
        int cellHeight = 0;
        for (int i = 0; i < emojis.length; i++) {
            entries[i] = findAlphaBounds(emojis[i]);
            cellWidth = Math.max(cellWidth, entries[i].width);
            cellHeight = Math.max(cellHeight, entries[i].height);
        }

        int atlasWidth = cellWidth * ATLAS_COLUMNS;
        int atlasHeight = cellHeight * ATLAS_ROWS;
        Bitmap atlas = Bitmap.createBitmap(atlasWidth, atlasHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(atlas);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawColor(Color.TRANSPARENT);
        for (int i = 0; i < emojis.length; i++) {
            int column = i % ATLAS_COLUMNS;
            int row = i / ATLAS_COLUMNS;
            AtlasEntry entry = entries[i];
            int atlasLeft = column * cellWidth;
            int atlasTop = row * cellHeight;
            Rect source = new Rect(entry.left, entry.top,
                    entry.left + entry.width, entry.top + entry.height);
            Rect destination = new Rect(atlasLeft, atlasTop,
                    atlasLeft + entry.width, atlasTop + entry.height);
            canvas.drawBitmap(emojis[i], source, destination, paint);

            entry.u = atlasLeft / (float) atlasWidth;
            entry.v = atlasTop / (float) atlasHeight;
            entry.uSize = entry.width / (float) atlasWidth;
            entry.vSize = entry.height / (float) atlasHeight;
        }

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, atlas, 0);
        atlas.recycle();
        return new Atlas(textures[0], entries);
    }

    private static AtlasEntry findAlphaBounds(Bitmap bitmap) {
        int minX = bitmap.getWidth();
        int minY = bitmap.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) > ALPHA_THRESHOLD) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new AtlasEntry(0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    bitmap.getWidth(), bitmap.getHeight());
        }

        return new AtlasEntry(minX, minY, maxX - minX + 1, maxY - minY + 1,
                bitmap.getWidth(), bitmap.getHeight());
    }

    private static FloatBuffer allocateBuffer(int floats) {
        return ByteBuffer.allocateDirect(floats * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    private static FloatBuffer allocateQuadBuffer() {
        FloatBuffer buffer = allocateBuffer(QUAD_VERTICES * FLOATS_PER_QUAD_VERTEX);
        float[] vertices = {
                -0.5f, -0.5f, 0f, 0f,
                0.5f, -0.5f, 1f, 0f,
                -0.5f, 0.5f, 0f, 1f,
                0.5f, -0.5f, 1f, 0f,
                0.5f, 0.5f, 1f, 1f,
                -0.5f, 0.5f, 0f, 1f
        };
        buffer.put(vertices);
        buffer.position(0);
        return buffer;
    }

    private static int createProgram(String vertexShader, String fragmentShader) {
        int vertex = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fragment = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertex);
        GLES20.glAttachShader(program, fragment);
        GLES20.glLinkProgram(program);
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private static final String VERTEX_SHADER =
            "#version 300 es\n"
                    + "in vec2 aCorner;\n"
                    + "in vec2 aUvCorner;\n"
                    + "in vec4 aCenterDrawSize;\n"
                    + "in vec4 aAlphaRotation;\n"
                    + "in vec4 aUvRect;\n"
                    + "uniform vec2 uResolution;\n"
                    + "out vec2 vUv;\n"
                    + "out float vAlpha;\n"
                    + "void main() {\n"
                    + "  vec2 center = aCenterDrawSize.xy;\n"
                    + "  vec2 drawSize = aCenterDrawSize.zw;\n"
                    + "  vec2 rotation = aAlphaRotation.yz;\n"
                    + "  vec2 local = aCorner * drawSize;\n"
                    + "  vec2 rotated = vec2(local.x * rotation.x - local.y * rotation.y,\n"
                    + "                      local.x * rotation.y + local.y * rotation.x);\n"
                    + "  vec2 zeroToOne = (center + rotated) / uResolution;\n"
                    + "  vec2 clip = zeroToOne * 2.0 - 1.0;\n"
                    + "  gl_Position = vec4(clip.x, -clip.y, 0.0, 1.0);\n"
                    + "  vUv = aUvRect.xy + aUvCorner * aUvRect.zw;\n"
                    + "  vAlpha = aAlphaRotation.x;\n"
                    + "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 300 es\n"
                    + "precision mediump float;\n"
                    + "uniform sampler2D uTexture;\n"
                    + "in vec2 vUv;\n"
                    + "in float vAlpha;\n"
                    + "out vec4 fragColor;\n"
                    + "void main() {\n"
                    + "  vec4 color = texture(uTexture, vUv);\n"
                    + "  fragColor = vec4(color.rgb, color.a * vAlpha);\n"
                    + "}\n";

    private static final class Atlas {
        final int textureId;
        final AtlasEntry[] entries;

        Atlas(int textureId, AtlasEntry[] entries) {
            this.textureId = textureId;
            this.entries = entries;
        }
    }

    private static final class AtlasEntry {
        final int left;
        final int top;
        final int width;
        final int height;
        final int sourceWidth;
        final int sourceHeight;
        float u;
        float v;
        float uSize;
        float vSize;

        AtlasEntry(int left, int top, int width, int height, int sourceWidth, int sourceHeight) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
        }
    }
}
