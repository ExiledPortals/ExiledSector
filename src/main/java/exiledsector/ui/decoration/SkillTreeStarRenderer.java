package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AuroraRenderer;
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.Star;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.ColorUtil;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTreeStarRenderer {

    private static final float RIM_ALPHA_MULT = 0.37f;
    private static final String FALLBACK_STAR_TYPE = "star_yellow";

    private static final String AURORA_TEXTURE_CATEGORY = "terrain";
    private static final String AURORA_TEXTURE_ID = "aurora";
    private static final float AURORA_INNER_RADIUS_MULT = 1.1f;
    private static final float AURORA_OUTER_RADIUS_MULT = 1.6f;
    private static final float AURORA_MIN_THICKNESS = 10f;
    private static final float AURORA_VERTEX_WOBBLE = 50f * 0.33f;
    private static final float AURORA_SHORTEN_MULT = 0.85f;
    private static final int AURORA_ALPHA = 25;
    private static final float AURORA_BAND_WIDTH_IN_TEXTURE = 256f;

    private static final String ATMOSPHERE_TEXTURE_CATEGORY = "planets";
    private static final String ATMOSPHERE_TEXTURE_ID = "atmosphere2";
    private static final float ATMOSPHERE_INNER_INSET_MULT = 0.4f;
    private static final int ATMOSPHERE_SEGMENTS = 64;

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeStarRenderer.class);
    private final UnitSphere sphere = new UnitSphere();
    private final Map<String, Float> angleById = new HashMap<>();
    private final Map<String, AuroraRenderer> auroraById = new HashMap<>();
    private final Map<String, AuroraDelegate> auroraDelegateById = new HashMap<>();
    private Map<String, PlanetSpecAPI> specsByType;
    private SpriteAPI atmosphereTexture;
    private SpriteAPI auroraTexture;
    private final Map<String, Map<Color, Color>> resolvedColors = new HashMap<>();

    public void advance(float amount) {
        if (amount <= 0f) return;
        for (Star star : SkillTree.getStars()) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            if (spec == null) continue;
            float angle = normalizeAngle(angleById.getOrDefault(star.getId(), 0f) + spec.getRotation() * amount);
            angleById.put(star.getId(), angle);
            getOrCreateAurora(star).advance(amount);
        }
    }

    public void renderDisc(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        List<Star> stars = SkillTree.getStars();
        if (stars.isEmpty()) return;

        for (Star star : stars) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            String texturePath = spec == null ? null : spec.getTexture();
            SpriteAPI texture = texturePath == null || texturePath.isEmpty() ? null : spriteCache.texture(texturePath);
            if (texture == null) {
                continue;
            }

            float screenX = viewport.screenX(star.getX());
            float screenY = viewport.screenY(star.getY());
            float radius = star.getRadius() * zoom;
            if (!viewport.isVisible(screenX, screenY, radius + 0.5f * zoom)) {
                continue;
            }
            float angle = angleById.getOrDefault(star.getId(), 0f);
            Color discColor = resolveColor(star, spec.getPlanetColor());

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_CULL_FACE);
            // TODO: if the star renders inside-out in-game, swap GL11.GL_CW <-> GL11.GL_CCW here.
            GL11.glFrontFace(GL11.GL_CW);
            GL11.glCullFace(GL11.GL_BACK);

            GL11.glPushMatrix();
            GL11.glTranslatef(screenX, screenY, 0f);
            GL11.glRotatef(spec.getTilt(), 0f, 0f, 1f);
            GL11.glRotatef(spec.getPitch(), 1f, 0f, 0f);
            GL11.glRotatef(angle, 0f, 1f, 0f);
            GL11.glRotatef(-90f, 1f, 0f, 0f);
            texture.bindTexture();

            Misc.setColor(discColor, alphaMult);
            sphere.draw(radius);
            Misc.setColor(discColor, alphaMult * RIM_ALPHA_MULT);
            sphere.draw(radius + 0.25f * zoom);
            sphere.draw(radius + 0.5f * zoom);

            GL11.glPopMatrix();

            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
    }

    public void renderAtmosphere(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        List<Star> stars = SkillTree.getStars();
        if (stars.isEmpty()) return;

        if (atmosphereTexture == null) {
            atmosphereTexture = Global.getSettings().getSprite(ATMOSPHERE_TEXTURE_CATEGORY, ATMOSPHERE_TEXTURE_ID);
        }
        SpriteAPI texture = atmosphereTexture;
        if (texture == null) return;

        for (Star star : stars) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            if (spec == null || spec.getAtmosphereThickness() <= 0f) {
                continue;
            }

            float radius = star.getRadius() * zoom;
            float thickness = Math.max(star.getRadius() * spec.getAtmosphereThickness(), spec.getAtmosphereThicknessMin()) * zoom;
            if (thickness > 0f) {
                float innerRadius = radius - thickness * ATMOSPHERE_INNER_INSET_MULT;
                float outerRadius = innerRadius + thickness;

                float screenX = viewport.screenX(star.getX());
                float screenY = viewport.screenY(star.getY());
                if (viewport.isVisible(screenX, screenY, outerRadius)) {
                    drawAtmosphereRing(texture, screenX, screenY, innerRadius, outerRadius, resolveColor(star, spec.getAtmosphereColor()), alphaMult);
                }
            }
        }
    }

    private void drawAtmosphereRing(SpriteAPI texture, float centerX, float centerY, float innerRadius, float outerRadius, Color color, float alphaMult) {
        UnitCircle circle = UnitCircle.of(ATMOSPHERE_SEGMENTS);

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        texture.bindTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        Misc.setColor(color, alphaMult);

        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= ATMOSPHERE_SEGMENTS; i++) {
            float cos = circle.cos(i % ATMOSPHERE_SEGMENTS);
            float sin = circle.sin(i % ATMOSPHERE_SEGMENTS);
            GL11.glTexCoord2f(0f, 0f);
            GL11.glVertex2f(cos * innerRadius, sin * innerRadius);
            GL11.glTexCoord2f(0f, 0.99f);
            GL11.glVertex2f(cos * outerRadius, sin * outerRadius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    public void renderAurora(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        List<Star> stars = SkillTree.getStars();
        if (stars.isEmpty()) return;

        if (auroraTexture == null) {
            auroraTexture = Global.getSettings().getSprite(AURORA_TEXTURE_CATEGORY, AURORA_TEXTURE_ID);
        }
        SpriteAPI texture = auroraTexture;
        if (texture == null) return;

        for (Star star : stars) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            if (spec == null) continue;

            float radius = star.getRadius() * zoom;

            float screenX = viewport.screenX(star.getX());
            float screenY = viewport.screenY(star.getY());
            if (!viewport.isVisible(screenX, screenY, auroraReach(radius))) {
                continue;
            }
            Color coronaColor = resolveColor(star, spec.getCoronaColor());

            AuroraRenderer renderer = getOrCreateAurora(star);
            AuroraDelegate delegate = auroraDelegateById.get(star.getId());
            delegate.centerLoc.set(screenX, screenY);
            delegate.innerRadius = radius * AURORA_INNER_RADIUS_MULT;
            delegate.outerRadius = radius * AURORA_OUTER_RADIUS_MULT;
            delegate.color = Misc.setAlpha(coronaColor, AURORA_ALPHA);
            delegate.texture = texture;

            renderer.render(alphaMult);

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
    }

    private static float auroraReach(float radius) {
        float inner = radius * AURORA_INNER_RADIUS_MULT;
        float thickness = Math.max(radius * AURORA_OUTER_RADIUS_MULT - inner, AURORA_MIN_THICKNESS);
        return inner + thickness * (1f + AURORA_SHORTEN_MULT) + AURORA_VERTEX_WOBBLE;
    }

    private AuroraRenderer getOrCreateAurora(Star star) {
        AuroraRenderer renderer = auroraById.get(star.getId());
        if (renderer == null) {
            AuroraDelegate delegate = new AuroraDelegate();
            renderer = new AuroraRenderer(delegate);
            auroraDelegateById.put(star.getId(), delegate);
            auroraById.put(star.getId(), renderer);
        }
        return renderer;
    }

    public void renderGlow(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        List<Star> stars = SkillTree.getStars();
        if (stars.isEmpty()) return;

        for (Star star : stars) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            String coronaPath = spec == null ? null : spec.getCoronaTexture();
            if (coronaPath == null || coronaPath.isEmpty()) {
                continue;
            }

            float radius = star.getRadius() * zoom;
            float haloRadius = star.getRadius() * spec.getCoronaSize() * zoom;
            float screenX = viewport.screenX(star.getX());
            float screenY = viewport.screenY(star.getY());
            if (haloRadius > radius && viewport.isVisible(screenX, screenY, haloRadius)) {
                SpriteDraw.drawAdditiveAtCenter(spriteCache, coronaPath, screenX, screenY,
                        haloRadius * 2f, haloRadius * 2f, resolveColor(star, spec.getCoronaColor()), alphaMult);
            }
        }
    }

    private Color resolveColor(Star star, Color fallback) {
        Map<Color, Color> byFallback = resolvedColors.get(star.getId());
        if (byFallback == null) {
            byFallback = new HashMap<>();
            resolvedColors.put(star.getId(), byFallback);
        }
        Color resolved = byFallback.get(fallback);
        if (resolved == null) {
            Color parsed = ColorUtil.parseHexColor(star.getColor(), fallback);
            resolved = new Color(parsed.getRed(), parsed.getGreen(), parsed.getBlue(), fallback.getAlpha());
            byFallback.put(fallback, resolved);
        }
        return resolved;
    }

    private PlanetSpecAPI resolveSpec(String starType) {
        if (specsByType == null) {
            specsByType = new HashMap<>();
            for (PlanetSpecAPI spec : Global.getSettings().getAllPlanetSpecs()) {
                specsByType.put(spec.getPlanetType(), spec);
            }
        }
        PlanetSpecAPI spec = specsByType.get(starType);
        return spec != null ? spec : specsByType.get(FALLBACK_STAR_TYPE);
    }

    private static float normalizeAngle(float angle) {
        angle %= 360f;
        return angle < 0f ? angle + 360f : angle;
    }

    private static final class AuroraDelegate implements AuroraRenderer.AuroraRendererDelegate {
        private final Vector2f centerLoc = new Vector2f();
        private float innerRadius;
        private float outerRadius;
        private Color color = Color.WHITE;
        private SpriteAPI texture;

        @Override
        public float getAuroraInnerRadius() {
            return innerRadius;
        }

        @Override
        public float getAuroraOuterRadius() {
            return outerRadius;
        }

        @Override
        public Vector2f getAuroraCenterLoc() {
            return centerLoc;
        }

        @Override
        public Color getAuroraColorForAngle(float angle) {
            return color;
        }

        @Override
        public float getAuroraAlphaMultForAngle(float angle) {
            return 1f;
        }

        @Override
        public float getAuroraShortenMult(float angle) {
            return AURORA_SHORTEN_MULT;
        }

        @Override
        public float getAuroraInnerOffsetMult(float angle) {
            return 1f;
        }

        @Override
        public float getAuroraThicknessMult(float angle) {
            return 1f;
        }

        @Override
        public float getAuroraThicknessFlat(float angle) {
            return 0f;
        }

        @Override
        public float getAuroraTexPerSegmentMult() {
            return 1f;
        }

        @Override
        public float getAuroraBandWidthInTexture() {
            return AURORA_BAND_WIDTH_IN_TEXTURE;
        }

        @Override
        public SpriteAPI getAuroraTexture() {
            return texture;
        }

        @Override
        public RangeBlockerUtil getAuroraBlocker() {
            return null;
        }
    }
}
