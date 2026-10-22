package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.SkillNode;
import exiledsector.skills.radius.NodeRadii;
import exiledsector.skills.radius.NodeRadiusCache;
import exiledsector.skills.radius.RadiusSize;
import exiledlib.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;
import exiledlib.ui.util.GlScope;
import exiledlib.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class NodeRadiusRingRenderer {

    private static final int RING_SEGMENTS = 128;
    private static final UnitCircle RING_CIRCLE = UnitCircle.of(RING_SEGMENTS);
    private static final Color RING_COLOR = SkillTreePanelStyle.GLOW_COLOR;
    private static final float RING_FILL_ALPHA = 0.06f;
    private static final float RING_LINE_ALPHA = 0.8f;
    private static final float RING_LINE_THICKNESS = 2f;
    private static final String BAND_TEXTURE_CATEGORY = "abilities";
    private static final String BAND_TEXTURE_ID = "neutrino_detector";
    private static final float BAND_WIDTH_IN_TEXTURE = 256f;
    private static final int BAND_SEGMENTS = 256;
    private static final float BAND_THICKNESS_SHARE = 0.09f;
    private static final float BAND_PULSE_SHARE = 0.25f;
    private static final float BAND_EDGE_TEXTURE_TRIM = 0.2f;
    private static final float BAND_SHIMMER_SHARE = 0.33f;
    private static final float BAND_ALPHA = 0.55f;
    private static final float BAND_PHASE_DEGREES_PER_SECOND = 60f;
    private static final float FIRST_BAND_WAVES = 29f;
    private static final float SECOND_BAND_WAVES = 17f;

    private SkillNode ringCentre;
    private float ringWorldRadius;
    private SkillNode socketedCentre;
    private float socketedWorldRadius;
    private Color socketedColor = RING_COLOR;
    private float bandPhaseDegrees;
    private SpriteAPI bandTexture;

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

    public void setSocketedRadius(SkillNode centre, RadiusSize size, Color color) {
        boolean shown = centre != null && size != null && !NodeRadiusCache.isExcluded(centre);
        socketedCentre = shown ? centre : null;
        socketedWorldRadius = shown ? NodeRadii.distance(size) : 0f;
        socketedColor = color == null ? RING_COLOR : color;
    }

    public void advance(float amount) {
        if (socketedCentre == null) {
            return;
        }
        bandPhaseDegrees = (bandPhaseDegrees + BAND_PHASE_DEGREES_PER_SECOND * amount) % 360f;
    }

    public void renderSocketed(TreeLayers layers) {
        if (socketedCentre == null) {
            return;
        }
        TreeViewport viewport = layers.viewportFor(socketedCentre);
        float layerAlpha = layers.alphaFor(socketedCentre);
        float screenRadius = socketedWorldRadius * viewport.zoom();
        if (layerAlpha <= 0f || screenRadius <= 0f) {
            return;
        }
        float centreX = viewport.screenX(socketedCentre.getOffsetX());
        float centreY = viewport.screenY(socketedCentre.getOffsetY());
        if (!viewport.isVisible(centreX, centreY, screenRadius)) {
            return;
        }
        if (bandTexture == null) {
            bandTexture = Global.getSettings().getSprite(BAND_TEXTURE_CATEGORY, BAND_TEXTURE_ID);
        }
        if (bandTexture != null) {
            try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {
                GL11.glPushMatrix();
                GL11.glTranslatef(centreX, centreY, 0f);
                GL11.glScalef(viewport.zoom(), viewport.zoom(), 1f);
                bandTexture.bindTexture();
                drawBand(bandTexture, 1, FIRST_BAND_WAVES, bandPhaseDegrees, 0f, layerAlpha);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                drawBand(bandTexture, 0, SECOND_BAND_WAVES, -bandPhaseDegrees, 0.5f, layerAlpha);
                GL11.glPopMatrix();
            }
        }
    }

    private void drawBand(SpriteAPI texture, int bandIndex, float waves, float phaseDegrees, float textureStartShare, float layerAlpha) {
        float outerRadius = socketedWorldRadius;
        float thickness = outerRadius * BAND_THICKNESS_SHARE;
        float anglePerSegment = (float) (Math.PI * 2 / BAND_SEGMENTS);
        float worldPerSegment = (outerRadius - thickness / 2f) * anglePerSegment;
        float shownBandWidth = BAND_WIDTH_IN_TEXTURE * (1f - BAND_EDGE_TEXTURE_TRIM);
        float rawTexPerSegment = worldPerSegment * texture.getTextureHeight() / texture.getHeight() * shownBandWidth / thickness;
        float texPerSegment = Math.max(1f, Math.round(rawTexPerSegment * BAND_SEGMENTS)) / BAND_SEGMENTS;
        float bandTexWidth = texture.getTextureWidth() * BAND_WIDTH_IN_TEXTURE / texture.getWidth();
        float edgeTX = (bandIndex + BAND_EDGE_TEXTURE_TRIM) * bandTexWidth;
        float innerTX = (bandIndex + 1) * bandTexWidth - 0.001f;
        float phaseRadians = (float) Math.toRadians(phaseDegrees);
        float texProgress = textureStartShare * BAND_SEGMENTS * texPerSegment;
        float alpha = BAND_ALPHA * layerAlpha;
        GL11.glColor4f(socketedColor.getRed() / 255f, socketedColor.getGreen() / 255f, socketedColor.getBlue() / 255f, alpha);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= BAND_SEGMENTS; i++) {
            float theta = anglePerSegment * (i % BAND_SEGMENTS);
            float wave = phaseRadians + theta * waves;
            float pulse = (float) Math.sin(wave) * thickness * BAND_PULSE_SHARE;
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);
            float innerRadius = outerRadius - thickness + pulse;
            float shimmer = worldPerSegment * BAND_SHIMMER_SHARE;
            GL11.glTexCoord2f(edgeTX, texProgress);
            GL11.glVertex2f(cos * outerRadius, sin * outerRadius);
            GL11.glTexCoord2f(innerTX, texProgress);
            GL11.glVertex2f(cos * innerRadius + (float) Math.cos(wave) * shimmer, sin * innerRadius + (float) Math.sin(wave) * shimmer);
            texProgress += texPerSegment;
        }
        GL11.glEnd();
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
