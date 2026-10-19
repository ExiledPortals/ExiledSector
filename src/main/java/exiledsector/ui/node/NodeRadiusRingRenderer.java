package exiledsector.ui.node;

import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.SkillNode;
import exiledsector.skills.radius.NodeRadii;
import exiledsector.skills.radius.NodeRadiusCache;
import exiledsector.skills.radius.RadiusSize;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class NodeRadiusRingRenderer {

    private static final int RING_SEGMENTS = 128;
    private static final UnitCircle RING_CIRCLE = UnitCircle.of(RING_SEGMENTS);
    private static final Color RING_COLOR = SkillTreePanelStyle.GLOW_COLOR;
    private static final float RING_FILL_ALPHA = 0.06f;
    private static final float RING_LINE_ALPHA = 0.8f;
    private static final float RING_LINE_THICKNESS = 2f;

    private SkillNode ringCentre;
    private float ringWorldRadius;

    public void setHighlightedRadius(SkillNode centre, RadiusSize size) {
        if (centre == null || size == null || NodeRadiusCache.isExcluded(centre)) {
            clearHighlightedRadius();
            return;
        }
        ringCentre = centre;
        ringWorldRadius = NodeRadii.distance(size);
    }

    public void clearHighlightedRadius() {
        ringCentre = null;
    }

    public void render(TreeLayers layers) {
        if (ringCentre == null) {
            return;
        }
        TreeViewport viewport = layers.viewportFor(ringCentre);
        float ringAlpha = layers.alphaFor(ringCentre);
        float screenRadius = ringWorldRadius * viewport.zoom();
        if (ringAlpha <= 0f || screenRadius <= 0f) {
            return;
        }
        float centreX = viewport.screenX(ringCentre.getOffsetX());
        float centreY = viewport.screenY(ringCentre.getOffsetY());
        if (!viewport.isVisible(centreX, centreY, screenRadius)) {
            return;
        }
        try (GlScope scope = GlScope.flat().lineWidth(RING_LINE_THICKNESS)) {
            Misc.setColor(RING_COLOR, RING_FILL_ALPHA * ringAlpha);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(centreX, centreY);
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                circleVertex(i % RING_SEGMENTS, centreX, centreY, screenRadius);
            }
            GL11.glEnd();
            Misc.setColor(RING_COLOR, RING_LINE_ALPHA * ringAlpha);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < RING_SEGMENTS; i++) {
                circleVertex(i, centreX, centreY, screenRadius);
            }
            GL11.glEnd();
        }
    }

    private static void circleVertex(int index, float centreX, float centreY, float screenRadius) {
        GL11.glVertex2f(centreX + RING_CIRCLE.cos(index) * screenRadius, centreY + RING_CIRCLE.sin(index) * screenRadius);
    }
}
