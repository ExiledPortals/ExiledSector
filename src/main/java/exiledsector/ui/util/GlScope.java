package exiledsector.ui.util;

import org.lwjgl.opengl.GL11;

public final class GlScope implements AutoCloseable {

    public static final int DRAW_ATTRIBS = GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT;
    public static final int CLIP_ATTRIBS = GL11.GL_SCISSOR_BIT;

    interface Backend {

        void pushAttrib(int attribMask);

        void popAttrib();

        void enable(int capability);

        void disable(int capability);

        void blendFunc(int sourceFactor, int destinationFactor);

        void lineWidth(float lineWidth);

        void scissor(int scissorX, int scissorY, int scissorWidth, int scissorHeight);
    }

    private static final Backend LWJGL_BACKEND = new Backend() {
        @Override
        public void pushAttrib(int attribMask) {
            GL11.glPushAttrib(attribMask);
        }

        @Override
        public void popAttrib() {
            GL11.glPopAttrib();
        }

        @Override
        public void enable(int capability) {
            GL11.glEnable(capability);
        }

        @Override
        public void disable(int capability) {
            GL11.glDisable(capability);
        }

        @Override
        public void blendFunc(int sourceFactor, int destinationFactor) {
            GL11.glBlendFunc(sourceFactor, destinationFactor);
        }

        @Override
        public void lineWidth(float lineWidth) {
            GL11.glLineWidth(lineWidth);
        }

        @Override
        public void scissor(int scissorX, int scissorY, int scissorWidth, int scissorHeight) {
            GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        }
    };

    private static final GlScope OPEN_SCOPE = new GlScope();
    private static Backend backend = LWJGL_BACKEND;

    private GlScope() {
    }

    public static GlScope save(int attribMask) {
        backend.pushAttrib(attribMask);
        return OPEN_SCOPE;
    }

    public static GlScope flat() {
        return save(DRAW_ATTRIBS).untextured().blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static GlScope textured(int sourceFactor, int destinationFactor) {
        return save(DRAW_ATTRIBS).enable(GL11.GL_TEXTURE_2D).blend(sourceFactor, destinationFactor);
    }

    public static GlScope clip(int scissorX, int scissorY, int scissorWidth, int scissorHeight) {
        GlScope scope = save(CLIP_ATTRIBS).enable(GL11.GL_SCISSOR_TEST);
        backend.scissor(scissorX, scissorY, scissorWidth, scissorHeight);
        return scope;
    }

    public static void restore() {
        backend.popAttrib();
    }

    public GlScope enable(int capability) {
        backend.enable(capability);
        return this;
    }

    public GlScope disable(int capability) {
        backend.disable(capability);
        return this;
    }

    public GlScope untextured() {
        return disable(GL11.GL_TEXTURE_2D);
    }

    public GlScope blend(int sourceFactor, int destinationFactor) {
        backend.enable(GL11.GL_BLEND);
        backend.blendFunc(sourceFactor, destinationFactor);
        return this;
    }

    public GlScope additive() {
        return blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    public GlScope normalBlend() {
        return blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public GlScope lineWidth(float lineWidth) {
        backend.lineWidth(lineWidth);
        return this;
    }

    @Override
    public void close() {
        restore();
    }

    static Backend useBackend(Backend replacement) {
        Backend previous = backend;
        backend = replacement;
        return previous;
    }
}
