package exiledsector.ui.atlas;

import exiledsector.ui.decoration.DepthLens;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AtlasRegionBubble {

    static final float MESH_STEP_CELLS = 0.25f;
    static final float LIFT_PER_CELL = 0.5f;
    static final float MOST_LIFT = 1.2f;
    static final long TEXTURE_BUDGET_BYTES = 48L << 20;
    private static final Map<AtlasRegionBubble, Long> UPLOADED = new LinkedHashMap<>(16, 0.75f, true);
    private static long uploadedBytes;

    private final List<float[]> outlines;
    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;
    private final Color tint;
    private final float[] placed = new float[3];
    private AtlasBubbleField field;
    private int texture;
    private float textureWidth;
    private float textureHeight;
    private int columns;
    private float[] meshX;
    private float[] meshY;
    private float[] meshLift;
    private float[] meshGround;
    private float[] meshScreen;
    private int[] usedVertices;
    private int[] filledQuads;

    AtlasRegionBubble(List<float[]> outlines, float minX, float minY, float maxX, float maxY, Color tint) {
        this.outlines = outlines;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.tint = tint;
    }

    void draw(DepthLens lens, float alpha) {
        if (field == null) {
            field = AtlasBubbleField.of(outlines, minX, minY, maxX, maxY);
            buildMesh();
        }
        if (filledQuads.length == 0) {
            return;
        }
        if (texture == 0) {
            upload();
        } else {
            markRecentlyDrawn();
        }
        if (meshGround == null) {
            meshGround = new float[meshX.length];
            for (int vertex : usedVertices) {
                meshGround[vertex] = lens.depthAt(meshX[vertex], meshY[vertex]);
            }
        }
        for (int vertex : usedVertices) {
            lens.place(meshX[vertex], meshY[vertex], meshGround[vertex] + meshLift[vertex], placed);
            meshScreen[vertex * 2] = placed[DepthLens.X];
            meshScreen[vertex * 2 + 1] = placed[DepthLens.Y];
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glColor4f(1f, 1f, 1f, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        for (int quad : filledQuads) {
            corner(quad);
            corner(quad + 1);
            corner(quad + 1 + columns);
            corner(quad + columns);
        }
        GL11.glEnd();
    }

    void release() {
        if (texture != 0) {
            Long bytes = UPLOADED.remove(this);
            uploadedBytes -= bytes == null ? 0L : bytes;
            deleteTexture();
        }
    }

    private void markRecentlyDrawn() {
        UPLOADED.put(this, UPLOADED.remove(this));
    }

    private void deleteTexture() {
        GL11.glDeleteTextures(texture);
        texture = 0;
    }

    private void corner(int vertex) {
        GL11.glTexCoord2f((meshX[vertex] - field.originX()) / (field.texelSize() * textureWidth),
                (meshY[vertex] - field.originY()) / (field.texelSize() * textureHeight));
        GL11.glVertex2f(meshScreen[vertex * 2], meshScreen[vertex * 2 + 1]);
    }

    private void buildMesh() {
        float step = MESH_STEP_CELLS * AtlasMapCanvas.CELL_PIXELS;
        float startX = field.originX();
        float startY = field.originY();
        columns = (int) Math.ceil(field.width() * field.texelSize() / step) + 1;
        int rows = (int) Math.ceil(field.height() * field.texelSize() / step) + 1;
        meshX = new float[columns * rows];
        meshY = new float[columns * rows];
        meshLift = new float[columns * rows];
        meshScreen = new float[columns * rows * 2];
        float lift = Math.min(MOST_LIFT, LIFT_PER_CELL * field.peak() / AtlasMapCanvas.CELL_PIXELS);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int vertex = row * columns + column;
                meshX[vertex] = startX + column * step;
                meshY[vertex] = startY + row * step;
                meshLift[vertex] = lift * field.heightShareAt(meshX[vertex], meshY[vertex]);
            }
        }
        List<Integer> quads = new ArrayList<>();
        boolean[] used = new boolean[columns * rows];
        for (int row = 0; row + 1 < rows; row++) {
            for (int column = 0; column + 1 < columns; column++) {
                int vertex = row * columns + column;
                if (field.coversAny(meshX[vertex], meshY[vertex], meshX[vertex] + step, meshY[vertex] + step)) {
                    quads.add(vertex);
                    used[vertex] = true;
                    used[vertex + 1] = true;
                    used[vertex + columns] = true;
                    used[vertex + columns + 1] = true;
                }
            }
        }
        filledQuads = quads.stream().mapToInt(Integer::intValue).toArray();
        List<Integer> vertices = new ArrayList<>();
        for (int vertex = 0; vertex < used.length; vertex++) {
            if (used[vertex]) {
                vertices.add(vertex);
            }
        }
        usedVertices = vertices.stream().mapToInt(Integer::intValue).toArray();
    }

    private void upload() {
        int width = powerOfTwo(field.width());
        int height = powerOfTwo(field.height());
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        field.writePixels(tint, pixels, width);
        texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        textureWidth = width;
        textureHeight = height;
        long bytes = (long) width * height * 4;
        UPLOADED.put(this, bytes);
        uploadedBytes += bytes;
        Iterator<Map.Entry<AtlasRegionBubble, Long>> oldest = UPLOADED.entrySet().iterator();
        while (uploadedBytes > TEXTURE_BUDGET_BYTES && oldest.hasNext()) {
            Map.Entry<AtlasRegionBubble, Long> entry = oldest.next();
            if (entry.getKey() != this) {
                entry.getKey().deleteTexture();
                uploadedBytes -= entry.getValue();
                oldest.remove();
            }
        }
    }

    static int powerOfTwo(int size) {
        int power = 1;
        while (power < size) {
            power <<= 1;
        }
        return power;
    }
}
