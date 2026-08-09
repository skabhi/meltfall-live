package com.anderson.singh.play.meltfalllive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

final class GlRainRenderer {
    private static final int FLOATS_PER_VERTEX = 5;
    private static final int VERTICES_PER_DROP = 6;
    private static final int ATLAS_COLUMNS = 4;
    private static final int ATLAS_ROWS = 2;

    private final Context context;
    private final RainScene scene;

    private int program;
    private int textureId;
    private int positionHandle;
    private int uvHandle;
    private int alphaHandle;
    private int resolutionHandle;
    private int textureHandle;
    private int width;
    private int height;
    private FloatBuffer vertexBuffer = allocateBuffer(256);

    GlRainRenderer(Context context) {
        this.context = context.getApplicationContext();
        this.scene = new RainScene(context);
    }

    void onSurfaceCreated() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        uvHandle = GLES20.glGetAttribLocation(program, "aUv");
        alphaHandle = GLES20.glGetAttribLocation(program, "aAlpha");
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
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
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERTEX * 4, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        vertexBuffer.position(2);
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERTEX * 4, vertexBuffer);
        GLES20.glEnableVertexAttribArray(uvHandle);

        vertexBuffer.position(4);
        GLES20.glVertexAttribPointer(alphaHandle, 1, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERTEX * 4, vertexBuffer);
        GLES20.glEnableVertexAttribArray(alphaHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
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

        int requiredFloats = Math.max(1, visibleDrops * VERTICES_PER_DROP * FLOATS_PER_VERTEX);
        if (vertexBuffer.capacity() < requiredFloats) {
            vertexBuffer = allocateBuffer(requiredFloats);
        }

        vertexBuffer.clear();
        int vertexCount = 0;
        for (RainScene.Drop drop : scene.getDrops()) {
            if (!isVisible(drop)) {
                continue;
            }
            addDropVertices(drop);
            vertexCount += VERTICES_PER_DROP;
        }
        vertexBuffer.position(0);
        return vertexCount;
    }

    private void addDropVertices(RainScene.Drop drop) {
        float half = drop.size / 2f;
        float centerX = drop.x + half;
        float centerY = drop.y + half;
        double radians = Math.toRadians(drop.rotation);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float[] points = {
                -half, -half,
                half, -half,
                -half, half,
                half, -half,
                half, half,
                -half, half
        };

        float column = drop.emojiIndex % ATLAS_COLUMNS;
        float row = drop.emojiIndex / ATLAS_COLUMNS;
        float u0 = column / ATLAS_COLUMNS;
        float v0 = row / ATLAS_ROWS;
        float u1 = (column + 1f) / ATLAS_COLUMNS;
        float v1 = (row + 1f) / ATLAS_ROWS;
        float[] uvs = {
                u0, v0,
                u1, v0,
                u0, v1,
                u1, v0,
                u1, v1,
                u0, v1
        };

        for (int i = 0; i < VERTICES_PER_DROP; i++) {
            float localX = points[i * 2];
            float localY = points[i * 2 + 1];
            float x = centerX + localX * cos - localY * sin;
            float y = centerY + localX * sin + localY * cos;
            vertexBuffer.put(x);
            vertexBuffer.put(y);
            vertexBuffer.put(uvs[i * 2]);
            vertexBuffer.put(uvs[i * 2 + 1]);
            vertexBuffer.put(drop.alpha);
        }
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

    private static int createProgram(String vertexShader, String fragmentShader) {
        int vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
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
            "attribute vec2 aPosition;\n"
                    + "attribute vec2 aUv;\n"
                    + "attribute float aAlpha;\n"
                    + "uniform vec2 uResolution;\n"
                    + "varying vec2 vUv;\n"
                    + "varying float vAlpha;\n"
                    + "void main() {\n"
                    + "  vec2 zeroToOne = aPosition / uResolution;\n"
                    + "  vec2 clip = zeroToOne * 2.0 - 1.0;\n"
                    + "  gl_Position = vec4(clip.x, -clip.y, 0.0, 1.0);\n"
                    + "  vUv = aUv;\n"
                    + "  vAlpha = aAlpha;\n"
                    + "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "uniform sampler2D uTexture;\n"
                    + "varying vec2 vUv;\n"
                    + "varying float vAlpha;\n"
                    + "void main() {\n"
                    + "  vec4 color = texture2D(uTexture, vUv);\n"
                    + "  gl_FragColor = vec4(color.rgb, color.a * vAlpha);\n"
                    + "}\n";
}
