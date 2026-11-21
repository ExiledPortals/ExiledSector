package exiledsector.harness;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenCaptureTest {

    @Test
    void pixelsReadBottomRowFirstBecomeAnUprightPng() throws IOException {
        ByteBuffer rgbaPixels = ByteBuffer.wrap(new byte[] {
                (byte) 255, 0, 0, (byte) 255, 0, (byte) 255, 0, (byte) 255,
                0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255});

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(ScreenCapture.png(rgbaPixels, 2, 2)));

        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(0x0000FF, image.getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0xFFFFFF, image.getRGB(1, 0) & 0xFFFFFF);
        assertEquals(0xFF0000, image.getRGB(0, 1) & 0xFFFFFF);
        assertEquals(0x00FF00, image.getRGB(1, 1) & 0xFFFFFF);
    }
}
