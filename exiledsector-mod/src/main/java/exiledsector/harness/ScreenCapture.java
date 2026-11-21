package exiledsector.harness;

import com.fs.starfarer.api.campaign.listeners.CampaignUIRenderingListener;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

final class ScreenCapture implements CampaignUIRenderingListener {

    static final int FRONT_BUFFER_AFTER_FRAMES = 3;
    static final String END_OF_FRAME = "end of the UI frame";
    static final String FRONT_BUFFER = "front buffer";
    static final String COMBAT_FRAME = "combat frame below the combat HUD";
    private static final Logger LOG = Logger.getLogger(ScreenCapture.class);
    private static final int BYTES_PER_PIXEL = 4;

    private final HarnessResults results;
    private String requestedName;
    private int framesWaiting;
    private HarnessResults.Screenshot taken;
    private String failure;

    ScreenCapture(HarnessResults results) {
        this.results = results;
    }

    void request(String screenshotName) {
        requestedName = screenshotName;
        framesWaiting = 0;
        taken = null;
        failure = null;
    }

    boolean pending() {
        return requestedName != null;
    }

    HarnessResults.Screenshot taken() {
        return taken;
    }

    String failure() {
        return failure;
    }

    void advance() {
        if (requestedName == null) {
            return;
        }
        framesWaiting++;
        if (framesWaiting > FRONT_BUFFER_AFTER_FRAMES) {
            capture(GL11.GL_FRONT, FRONT_BUFFER);
        }
    }

    @Override
    public void renderInUICoordsAboveUIAndTooltips(ViewportAPI viewport) {
        if (requestedName != null) {
            capture(GL11.GL_BACK, END_OF_FRAME);
        }
    }

    void renderInCombat() {
        if (requestedName != null) {
            capture(GL11.GL_BACK, COMBAT_FRAME);
        }
    }

    @Override
    public void renderInUICoordsBelowUI(ViewportAPI viewport) {
    }

    @Override
    public void renderInUICoordsAboveUIBelowTooltips(ViewportAPI viewport) {
    }

    private void capture(int readBuffer, String source) {
        String screenshotName = requestedName;
        requestedName = null;
        try {
            int width = Display.getWidth();
            int height = Display.getHeight();
            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * BYTES_PER_PIXEL);
            if (readBuffer == GL11.GL_FRONT) {
                GL11.glReadBuffer(GL11.GL_FRONT);
                GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
                GL11.glReadBuffer(GL11.GL_BACK);
            } else {
                GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            }
            int parts = HarnessFiles.writeScreenshot(screenshotName, Base64.getEncoder().encodeToString(png(pixels, width, height)));
            taken = new HarnessResults.Screenshot(screenshotName, width, height, parts, source);
            results.add(taken);
        } catch (IOException | RuntimeException e) {
            LOG.error(Harness.LOG_TAG + "could not take screenshot " + screenshotName, e);
            failure = e.toString();
        }
    }

    static byte[] png(ByteBuffer rgbaPixels, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int offset = (row * width + column) * BYTES_PER_PIXEL;
                int red = rgbaPixels.get(offset) & 0xFF;
                int green = rgbaPixels.get(offset + 1) & 0xFF;
                int blue = rgbaPixels.get(offset + 2) & 0xFF;
                image.setRGB(column, height - 1 - row, (red << 16) | (green << 8) | blue);
            }
        }
        ByteSink sink = new ByteSink();
        ImageIO.write(image, "png", sink);
        return sink.bytes();
    }

    private static final class ByteSink extends OutputStream {

        private byte[] buffer = new byte[1 << 16];
        private int size;

        @Override
        public void write(int value) {
            ensureRoom(1);
            buffer[size++] = (byte) value;
        }

        @Override
        public void write(byte[] source, int offset, int length) {
            ensureRoom(length);
            System.arraycopy(source, offset, buffer, size, length);
            size += length;
        }

        private void ensureRoom(int extra) {
            if (size + extra > buffer.length) {
                buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, size + extra));
            }
        }

        byte[] bytes() {
            return Arrays.copyOf(buffer, size);
        }
    }
}
