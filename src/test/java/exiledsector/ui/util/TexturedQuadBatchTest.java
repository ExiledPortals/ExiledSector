package exiledsector.ui.util;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TexturedQuadBatchTest {

    private final List<String> calls = new ArrayList<>();
    private final SpriteAPI texture = mock(SpriteAPI.class);
    private GlScope.Backend previousScopeBackend;
    private TexturedQuadBatch.Backend previousQuadBackend;

    @BeforeEach
    void useRecorders() {
        when(texture.getTexX()).thenReturn(0f);
        when(texture.getTexY()).thenReturn(0f);
        when(texture.getTexWidth()).thenReturn(0.5f);
        when(texture.getTexHeight()).thenReturn(0.25f);
        previousScopeBackend = GlScope.useBackend(new RecordingScopeBackend());
        previousQuadBackend = TexturedQuadBatch.useBackend(new RecordingQuadBackend());
    }

    @AfterEach
    void restoreBackends() {
        GlScope.useBackend(previousScopeBackend);
        TexturedQuadBatch.useBackend(previousQuadBackend);
    }

    @Test
    void aBatchSavesStateBindsOnceDrawsEveryQuadThenRestores() {
        TexturedQuadBatch batch = new TexturedQuadBatch();
        try (TexturedQuadBatch quads = batch.beginNormal(texture)) {
            quads.quadAtCenter(10f, 20f, 4f, 2f, 0f, Color.WHITE, 1f);
            quads.quadAtCenter(30f, 40f, 4f, 2f, 0f, Color.WHITE, 1f);
        }

        assertEquals(List.of(
                "push " + GlScope.DRAW_ATTRIBS,
                "enable " + GL11.GL_TEXTURE_2D,
                "enable " + GL11.GL_BLEND,
                "blend " + GL11.GL_SRC_ALPHA + " " + GL11.GL_ONE_MINUS_SRC_ALPHA,
                "bind",
                "begin",
                "color 255 255 255 255",
                "tex 0.0 0.0", "vertex 8.0 19.0",
                "tex 0.0 0.25", "vertex 8.0 21.0",
                "tex 0.5 0.25", "vertex 12.0 21.0",
                "tex 0.5 0.0", "vertex 12.0 19.0",
                "color 255 255 255 255",
                "tex 0.0 0.0", "vertex 28.0 39.0",
                "tex 0.0 0.25", "vertex 28.0 41.0",
                "tex 0.5 0.25", "vertex 32.0 41.0",
                "tex 0.5 0.0", "vertex 32.0 39.0",
                "end",
                "pop"), calls);
    }

    @Test
    void additiveBatchesUseTheAdditiveBlend() {
        try (TexturedQuadBatch quads = new TexturedQuadBatch().beginAdditive(texture)) {
            assertEquals("blend " + GL11.GL_SRC_ALPHA + " " + GL11.GL_ONE, calls.get(3));
        }
    }

    @Test
    void theColourMatchesWhatASpriteWouldSetIncludingTheTintAlpha() {
        try (TexturedQuadBatch quads = new TexturedQuadBatch().beginNormal(texture)) {
            quads.quadAtCenter(0f, 0f, 2f, 2f, 0f, new Color(10, 20, 30, 200), 0.5f);
        }

        assertEquals("color 10 20 30 100", calls.get(6));
    }

    @Test
    void aRotatedQuadTurnsAboutItsCentreCounterClockwise() {
        try (TexturedQuadBatch quads = new TexturedQuadBatch().beginNormal(texture)) {
            quads.quadAtCenter(0f, 0f, 4f, 2f, 90f, Color.WHITE, 1f);
        }

        assertEquals("vertex 1.0 -2.0", rounded(calls.get(8)));
        assertEquals("vertex -1.0 -2.0", rounded(calls.get(10)));
        assertEquals("vertex -1.0 2.0", rounded(calls.get(12)));
        assertEquals("vertex 1.0 2.0", rounded(calls.get(14)));
    }

    @Test
    void closingTwiceRestoresStateOnlyOnce() {
        TexturedQuadBatch quads = new TexturedQuadBatch().beginNormal(texture);
        quads.close();
        quads.close();

        assertEquals(1, calls.stream().filter("pop"::equals).count());
    }

    @Test
    void beginningAnOpenBatchFails() {
        TexturedQuadBatch batch = new TexturedQuadBatch().beginNormal(texture);
        try {
            assertThrows(IllegalStateException.class, () -> batch.beginNormal(texture));
        } finally {
            batch.close();
        }
    }

    private static String rounded(String vertexCall) {
        String[] parts = vertexCall.split(" ");
        return parts[0] + " " + (float) Math.round(Float.parseFloat(parts[1])) + " " + (float) Math.round(Float.parseFloat(parts[2]));
    }

    private final class RecordingQuadBackend implements TexturedQuadBatch.Backend {

        @Override
        public void bindTexture(SpriteAPI boundTexture) {
            calls.add("bind");
        }

        @Override
        public void beginQuads() {
            calls.add("begin");
        }

        @Override
        public void color(byte red, byte green, byte blue, byte alpha) {
            calls.add("color " + (red & 0xFF) + " " + (green & 0xFF) + " " + (blue & 0xFF) + " " + (alpha & 0xFF));
        }

        @Override
        public void texCoord(float textureX, float textureY) {
            calls.add("tex " + textureX + " " + textureY);
        }

        @Override
        public void vertex(float x, float y) {
            calls.add("vertex " + x + " " + y);
        }

        @Override
        public void end() {
            calls.add("end");
        }
    }

    private final class RecordingScopeBackend implements GlScope.Backend {

        @Override
        public void pushAttrib(int attribMask) {
            calls.add("push " + attribMask);
        }

        @Override
        public void popAttrib() {
            calls.add("pop");
        }

        @Override
        public void enable(int capability) {
            calls.add("enable " + capability);
        }

        @Override
        public void disable(int capability) {
            calls.add("disable " + capability);
        }

        @Override
        public void blendFunc(int sourceFactor, int destinationFactor) {
            calls.add("blend " + sourceFactor + " " + destinationFactor);
        }

        @Override
        public void lineWidth(float lineWidth) {
            calls.add("lineWidth " + lineWidth);
        }

        @Override
        public void scissor(int scissorX, int scissorY, int scissorWidth, int scissorHeight) {
            calls.add("scissor " + scissorX + " " + scissorY + " " + scissorWidth + " " + scissorHeight);
        }
    }
}
