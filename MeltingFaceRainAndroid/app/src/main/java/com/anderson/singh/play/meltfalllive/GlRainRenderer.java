package com.anderson.singh.play.meltfalllive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

final class GlRainRenderer {
    private static final int FLOATS_PER_QUAD_VERTEX = 4;
    private static final int FLOATS_PER_INSTANCE = 8;
    private static final int QUAD_VERTICES = 6;
    private static final int ATLAS_COLUMNS = 4;
    private static final int ATLAS_ROWS = 2;

    private final Context context;
    private final RainScene scene;

    private int program;
    private int textureId;
    private int cornerHandle;
    private int uvCornerHandle;
    private int centerSizeAlphaHandle;
    private int rotationUvOriginHandle;
    private int resolutionHandle;
    private int atlasCellSizeHandle;
    private int textureHandle;
    private int width;
    private int height;
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
        centerSizeAlphaHandle = GLES20.glGetAttribLocation(program, "aCenterSizeAlpha");
        rotationUvOriginHandle = GLES20.glGetAttribLocation(program, "aRotationUvOrigin");
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
        atlasCellSizeHandle = GLES20.glGetUniformLocation(program, "uAtlasCellSize");
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        textureId = createAtlasTexture(scene.getEmojis());

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
        GLES20.glUniform2f(atlasCellSizeHandle, 1f / ATLAS_COLUMNS, 1f / ATLAS_ROWS);
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
        GLES20.glVertexAttribPointer(centerSizeAlphaHandle, 4, GLES20.GL_FLOAT, false,
                FLOATS_PER_INSTANCE * 4, instanceBuffer);
        GLES20.glEnableVertexAttribArray(centerSizeAlphaHandle);
        GLES30.glVertexAttribDivisor(centerSizeAlphaHandle, 1);

        instanceBuffer.position(4);
        GLES20.glVertexAttribPointer(rotationUvOriginHandle, 4, GLES20.GL_FLOAT, false,
                FLOATS_PER_INSTANCE * 4, instanceBuffer);
        GLES20.glEnableVertexAttribArray(rotationUvOriginHandle);
        GLES30.glVertexAttribDivisor(rotationUvOriginHandle, 1);

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

        float column = drop.emojiIndex % ATLAS_COLUMNS;
        float row = drop.emojiIndex / ATLAS_COLUMNS;
        float u0 = column / ATLAS_COLUMNS;
        float v0 = row / ATLAS_ROWS;

        instanceBuffer.put(centerX);
        instanceBuffer.put(centerY);
        instanceBuffer.put(drop.size);
        instanceBuffer.put(drop.alpha);
        instanceBuffer.put(cos);
        instanceBuffer.put(sin);
        instanceBuffer.put(u0);
        instanceBuffer.put(v0);
    }

    private boolean isVisible(RainScene.Drop drop) {
        float margin = drop.size;
        return drop.x < width + margin
                && drop.x + drop.size > -margin
                && drop.y < height + margin
                && drop.y + drop.size > -margin;
    }

    private int createAtlasTexture(Bitmap[] emojis) {
        int cellWidth = 0;
        int cellHeight = 0;
        for (Bitmap emoji : emojis) {
            cellWidth = Math.max(cellWidth, emoji.getWidth());
            cellHeight = Math.max(cellHeight, emoji.getHeight());
        }

        Bitmap atlas = Bitmap.createBitmap(cellWidth * ATLAS_COLUMNS,
                cellHeight * ATLAS_ROWS, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(atlas);
        canvas.drawColor(Color.TRANSPARENT);
        for (int i = 0; i < emojis.length; i++) {
            int column = i % ATLAS_COLUMNS;
            int row = i / ATLAS_COLUMNS;
            canvas.drawBitmap(emojis[i], column * cellWidth, row * cellHeight, null);
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
        return textures[0];
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
                    + "in vec4 aCenterSizeAlpha;\n"
                    + "in vec4 aRotationUvOrigin;\n"
                    + "uniform vec2 uResolution;\n"
                    + "uniform vec2 uAtlasCellSize;\n"
                    + "out vec2 vUv;\n"
                    + "out float vAlpha;\n"
                    + "void main() {\n"
                    + "  vec2 center = aCenterSizeAlpha.xy;\n"
                    + "  float size = aCenterSizeAlpha.z;\n"
                    + "  vec2 rotation = aRotationUvOrigin.xy;\n"
                    + "  vec2 local = aCorner * size;\n"
                    + "  vec2 rotated = vec2(local.x * rotation.x - local.y * rotation.y,\n"
                    + "                      local.x * rotation.y + local.y * rotation.x);\n"
                    + "  vec2 zeroToOne = (center + rotated) / uResolution;\n"
                    + "  vec2 clip = zeroToOne * 2.0 - 1.0;\n"
                    + "  gl_Position = vec4(clip.x, -clip.y, 0.0, 1.0);\n"
                    + "  vUv = aRotationUvOrigin.zw + aUvCorner * uAtlasCellSize;\n"
                    + "  vAlpha = aCenterSizeAlpha.w;\n"
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
}
