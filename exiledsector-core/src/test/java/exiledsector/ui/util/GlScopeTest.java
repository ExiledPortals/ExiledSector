package exiledsector.ui.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlScopeTest {

    private final RecordingBackend recorder = new RecordingBackend();
    private GlScope.Backend previousBackend;

    @BeforeEach
    void useRecorder() {
        previousBackend = GlScope.useBackend(recorder);
    }

    @AfterEach
    void restoreBackend() {
        GlScope.useBackend(previousBackend);
    }

    @Test
    void aFlatScopeSavesTheStateBeforeChangingItAndRestoresItOnClose() {
        try (GlScope scope = GlScope.flat().lineWidth(2f)) {
            recorder.calls.add("draw");
        }

        assertEquals(List.of(
                "push " + GlScope.DRAW_ATTRIBS,
                "disable " + GL11.GL_TEXTURE_2D,
                "enable " + GL11.GL_BLEND,
                "blend " + GL11.GL_SRC_ALPHA + " " + GL11.GL_ONE_MINUS_SRC_ALPHA,
                "lineWidth 2.0",
                "draw",
                "pop"), recorder.calls);
    }

    @Test
    void aTexturedScopeTurnsTexturingOnWithTheRequestedBlend() {
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            recorder.calls.add("draw");
        }

        assertEquals(List.of(
                "push " + GlScope.DRAW_ATTRIBS,
                "enable " + GL11.GL_TEXTURE_2D,
                "enable " + GL11.GL_BLEND,
                "blend " + GL11.GL_SRC_ALPHA + " " + GL11.GL_ONE,
                "draw",
                "pop"), recorder.calls);
    }

    @Test
    void aClipScopeSavesOnlyTheScissorStateAndSetsTheBox() {
        try (GlScope scope = GlScope.clip(10, 20, 300, 40)) {
            recorder.calls.add("draw");
        }

        assertEquals(List.of(
                "push " + GL11.GL_SCISSOR_BIT,
                "enable " + GL11.GL_SCISSOR_TEST,
                "scissor 10 20 300 40",
                "draw",
                "pop"), recorder.calls);
    }

    @Test
    void theStateIsRestoredEvenWhenDrawingThrows() {
        assertThrows(IllegalStateException.class, () -> {
            try (GlScope scope = GlScope.flat()) {
                throw new IllegalStateException("draw failed");
            }
        });

        assertEquals("pop", recorder.calls.get(recorder.calls.size() - 1));
        assertEquals(0, recorder.depth);
    }

    @Test
    void nestedScopesUnwindInReverseOrder() {
        try (GlScope outer = GlScope.save(GL11.GL_ENABLE_BIT)) {
            try (GlScope inner = GlScope.flat()) {
                assertEquals(2, recorder.depth);
            }
            assertEquals(1, recorder.depth);
        }

        assertEquals(0, recorder.depth);
    }

    @Test
    void aBeginEndPairBalancesLikeTheTryForm() {
        GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA).additive();
        assertEquals(1, recorder.depth);
        GlScope.restore();

        assertEquals(0, recorder.depth);
        assertEquals("blend " + GL11.GL_SRC_ALPHA + " " + GL11.GL_ONE, recorder.calls.get(recorder.calls.size() - 2));
    }

    private static final class RecordingBackend implements GlScope.Backend {

        final List<String> calls = new ArrayList<>();
        int depth;

        @Override
        public void pushAttrib(int attribMask) {
            depth++;
            calls.add("push " + attribMask);
        }

        @Override
        public void popAttrib() {
            depth--;
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
