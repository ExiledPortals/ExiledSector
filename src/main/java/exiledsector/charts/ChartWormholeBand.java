package exiledsector.charts;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

final class ChartWormholeBand {

    private static final float BAND_WIDTH_IN_TEXTURE = 64f;
    private static final int BAND_INDEX = 2;
    private static final float FLUCTUATION_ANGULAR_VELOCITY = 100f;
    private static final float FLUCTUATION_ANGLE_MULT = 10f;
    private static final float TEXTURE_EDGE_INSET = 0.001f;

    private final SpriteAPI bandTexture;
    private final float angularVelocity;
    private final List<Fluctuation> fluctuations = new ArrayList<>();
    private float bandWidth;
    private float innerRadius;
    private float pixelsPerSegment;
    private float maxFluctuation;
    private float bandAngle = (float) Math.random() * 360f;
    private float waveAngle = (float) Math.random() * 360f;

    ChartWormholeBand(SpriteAPI bandTexture, float bandWidth, float pixelsPerSegment, float innerRadius, float angularVelocity,
            float maxFluctuation) {
        this.bandTexture = bandTexture;
        this.bandWidth = bandWidth;
        this.pixelsPerSegment = pixelsPerSegment;
        this.innerRadius = innerRadius;
        this.angularVelocity = angularVelocity;
        this.maxFluctuation = maxFluctuation;
    }

    void advance(float amount) {
        bandAngle += angularVelocity * amount;
        waveAngle += FLUCTUATION_ANGULAR_VELOCITY * amount;
        for (Fluctuation fluctuation : fluctuations) {
            fluctuation.advance(amount);
        }
    }

    float bandWidth() {
        return bandWidth;
    }

    void setBandWidth(float bandWidth) {
        this.bandWidth = bandWidth;
    }

    void setInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
    }

    void setPixelsPerSegment(float pixelsPerSegment) {
        this.pixelsPerSegment = pixelsPerSegment;
    }

    void setMaxFluctuation(float maxFluctuation) {
        if (this.maxFluctuation != maxFluctuation) {
            fluctuations.clear();
        }
        this.maxFluctuation = maxFluctuation;
    }

    int segmentCount() {
        return Math.round((float) (Math.PI * 2 * innerRadius) / pixelsPerSegment);
    }

    private void updateFluctuations() {
        int segmentCount = segmentCount();
        if (fluctuations.size() == segmentCount) {
            return;
        }
        while (fluctuations.size() > segmentCount && segmentCount > 0) {
            fluctuations.remove(fluctuations.size() - 1);
        }
        while (fluctuations.size() < segmentCount) {
            fluctuations.add(new Fluctuation(maxFluctuation));
        }
    }

    void render(float centreX, float centreY, float alphaMult) {
        updateFluctuations();
        float segmentCount = segmentCount();
        float radiansPerSegment = (float) (Math.PI * 2) / segmentCount;
        float textureStepPerSegment = pixelsPerSegment * bandTexture.getTextureHeight() / bandTexture.getHeight()
                * BAND_WIDTH_IN_TEXTURE / bandWidth;
        textureStepPerSegment = Math.max(1f, Math.round(textureStepPerSegment * segmentCount)) / segmentCount;
        float textureLeft = BAND_INDEX * bandTexture.getTextureWidth() * BAND_WIDTH_IN_TEXTURE / bandTexture.getWidth();
        float textureRight = (BAND_INDEX + 1f) * bandTexture.getTextureWidth() * BAND_WIDTH_IN_TEXTURE / bandTexture.getWidth()
                - TEXTURE_EDGE_INSET;

        GL11.glPushMatrix();
        GL11.glTranslatef(centreX, centreY, 0f);
        GL11.glRotatef(bandAngle, 0f, 0f, 1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        bandTexture.bindTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glColor4ub((byte) 255, (byte) 255, (byte) 255, (byte) (int) (255f * alphaMult));
        float textureAlong = 0f;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int segment = 0; segment < segmentCount; segment++) {
            emitSegment(segment, radiansPerSegment, textureLeft, textureRight, textureAlong);
            textureAlong += textureStepPerSegment;
        }
        emitSegment(0, radiansPerSegment, textureLeft, textureRight, textureAlong);
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void emitSegment(int segment, float radiansPerSegment, float textureLeft, float textureRight, float textureAlong) {
        float segmentAngle = radiansPerSegment * segment;
        float cos = (float) Math.cos(segmentAngle);
        float sin = (float) Math.sin(segmentAngle);
        float offset = segment < fluctuations.size() ? fluctuations.get(segment).value() : 0f;
        offset *= (float) Math.sin(segmentAngle * FLUCTUATION_ANGLE_MULT + Math.toRadians(waveAngle));
        GL11.glTexCoord2f(textureLeft, textureAlong);
        GL11.glVertex2f(cos * (innerRadius + offset), sin * (innerRadius + offset));
        GL11.glTexCoord2f(textureRight, textureAlong);
        GL11.glVertex2f(cos * (innerRadius + bandWidth + offset), sin * (innerRadius + bandWidth + offset));
    }

    static final class Fluctuation {

        private final float limit;
        private final float rate;
        private float value;
        private float direction;

        Fluctuation(float maxFluctuation) {
            this.limit = maxFluctuation;
            this.rate = Math.abs(maxFluctuation * (1f + (float) Math.random()));
            this.direction = Math.random() < 0.5 ? -1f : 1f;
            this.value = 0f;
        }

        void advance(float amount) {
            value += amount * rate * direction;
            if (value > limit) {
                direction = -1f;
            } else if (value < -limit) {
                direction = 1f;
            }
        }

        float value() {
            return value;
        }
    }
}
