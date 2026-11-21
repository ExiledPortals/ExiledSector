package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class BandPlacement {

    static final BandPlacement LOCAL = new BandPlacement(null, 0f, 0f);

    private final PlaneProjection projection;
    private final float centreX;
    private final float centreY;
    private float turnCos = 1f;
    private float turnSin;
    private float turnDegrees;

    private BandPlacement(PlaneProjection projection, float centreX, float centreY) {
        this.projection = projection;
        this.centreX = centreX;
        this.centreY = centreY;
    }

    static BandPlacement of(PlaneProjection projection, float centreX, float centreY) {
        return new BandPlacement(projection, centreX, centreY);
    }

    void begin(SpriteAPI texture, int blendSrcFactor, int blendDstFactor, Color color, float alphaMult) {
        if (projection == null) {
            RadialBandGL.begin(texture, centreX, centreY, blendSrcFactor, blendDstFactor, color, alphaMult);
        } else {
            RadialBandGL.beginTextured(texture, blendSrcFactor, blendDstFactor, color, alphaMult);
        }
    }

    void turnBy(float degrees) {
        if (projection == null) {
            GL11.glRotatef(degrees, 0f, 0f, 1f);
            return;
        }
        turnDegrees += degrees;
        double turnRadians = Math.toRadians(turnDegrees);
        turnCos = (float) Math.cos(turnRadians);
        turnSin = (float) Math.sin(turnRadians);
    }

    void end() {
        if (projection == null) {
            RadialBandGL.end();
        } else {
            RadialBandGL.endTextured();
        }
    }

    float planeX(float localX, float localY) {
        return centreX + localX * turnCos - localY * turnSin;
    }

    float planeY(float localX, float localY) {
        return centreY + localX * turnSin + localY * turnCos;
    }

    void vertex(float localX, float localY) {
        if (projection == null) {
            GL11.glVertex2f(localX, localY);
            return;
        }
        float planeX = planeX(localX, localY);
        float planeY = planeY(localX, localY);
        GL11.glVertex2f(projection.screenX(planeX, planeY), projection.screenY(planeX, planeY));
    }
}
