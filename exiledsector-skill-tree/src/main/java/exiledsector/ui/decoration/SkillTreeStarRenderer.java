package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.decoration.Star;
import exiledsector.ui.SmoothZoom;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.ColorUtil;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillTreeStarRenderer implements DecorationRenderer {

    private static final float RIM_ALPHA_MULT = 0.37f;
    private static final String FALLBACK_STAR_TYPE = "star_yellow";

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeStarRenderer.class);
    private final UnitSphere unitSphere = new UnitSphere();
    private final Map<String, StarAnimation> animationById = new HashMap<>();
    private Map<String, PlanetSpecAPI> specsByType;
    private SpriteAPI atmosphereTexture;
    private SpriteAPI auroraTexture;
    private float mapRadius;
    private float mapAmount;
    private Set<String> starIdsHiddenOnMap = Set.of();
    private final Map<String, Map<Color, Color>> resolvedColors = new HashMap<>();
    private final List<Star> stars;

    SkillTreeStarRenderer(List<Star> stars) {
        this.stars = List.copyOf(stars);
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        if (stars.isEmpty()) return;
        DecorationScene.StarMap starMap = scene.starMap();
        mapRadius = starMap.radius();
        mapAmount = starMap.amount();
        starIdsHiddenOnMap = starMap.hiddenStarIds();
        switch (depth) {
            case STAR_DISC -> renderDisc(scene.viewport(), scene.sceneryAlpha());
            case STAR_ATMOSPHERE -> renderAtmosphere(scene.viewport(), scene.sceneryAlpha());
            case STAR_AURORA -> renderAurora(scene.viewport(), scene.sceneryAlpha());
            case STAR_GLOW -> renderGlow(scene.viewport(), scene.sceneryAlpha());
            default -> {
            }
        }
    }

    private float radiusOf(Star star) {
        if (starIdsHiddenOnMap.contains(star.getId())) {
            return star.getRadius();
        }
        return star.getRadius() + (mapRadius - star.getRadius()) * mapAmount;
    }

    private float alphaOf(Star star, float alphaMult) {
        return starIdsHiddenOnMap.contains(star.getId()) ? alphaMult * (1f - mapAmount) : alphaMult;
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f) return;
        for (Star star : stars) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            if (spec == null) continue;
            StarAnimation animation = animationOf(star);
            animation.rotationAngle = normalizeAngle(animation.rotationAngle + spec.getRotation() * amount);
            animation.aurora.advance(amount);
        }
    }

    private void renderDisc(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        for (Star star : stars) {
            float starAlpha = alphaOf(star, alphaMult);
            renderStarDisc(star, viewport, zoom, starAlpha);
            renderStarOverlay(star, viewport, zoom, starAlpha);
        }
    }

    private void renderStarOverlay(Star star, TreeViewport viewport, float zoom, float alphaMult) {
        Star.Overlay overlay = star.getOverlay();
        SpriteAPI sprite = overlay == null || alphaMult <= 0f ? null : spriteCache.sprite(overlay.path());
        if (sprite == null || sprite.getWidth() <= 0f) {
            return;
        }
        float screenX = viewport.screenX(star.getX());
        float screenY = viewport.screenY(star.getY());
        float overlayWidth = radiusOf(star) * 2f * overlay.size() * zoom;
        float overlayHeight = overlayWidth * sprite.getHeight() / sprite.getWidth();
        if (!viewport.isVisible(screenX, screenY, Math.max(overlayWidth, overlayHeight) / 2f)) {
            return;
        }
        PlanetSpecAPI spec = resolveSpec(star.getStarType());
        Color tint = spec == null ? Color.WHITE : resolveColor(star, spec.getPlanetColor());
        SpriteDraw.drawAtCenter(sprite, screenX, screenY, overlayWidth, overlayHeight, tint, alphaMult * overlay.alpha(), 0f);
    }

    private void renderStarDisc(Star star, TreeViewport viewport, float zoom, float alphaMult) {
        PlanetSpecAPI spec = resolveSpec(star.getStarType());
        String texturePath = spec == null ? null : spec.getTexture();
        SpriteAPI discTexture = texturePath == null || texturePath.isEmpty() ? null : spriteCache.texture(texturePath);
        if (discTexture == null) {
            return;
        }

        float screenX = viewport.screenX(star.getX());
        float screenY = viewport.screenY(star.getY());
        float discRadius = radiusOf(star) * zoom;
        if (!viewport.isVisible(screenX, screenY, discRadius + 0.5f * zoom)) {
            return;
        }
        StarAnimation animation = animationById.get(star.getId());
        float rotationAngle = animation == null ? 0f : animation.rotationAngle;
        Color discColor = resolveColor(star, spec.getPlanetColor());

        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS | GL11.GL_POLYGON_BIT).enable(GL11.GL_TEXTURE_2D).normalBlend()
                .enable(GL11.GL_CULL_FACE)) {
            // TODO: if the star renders inside-out in-game, swap GL11.GL_CW <-> GL11.GL_CCW here.
            GL11.glFrontFace(GL11.GL_CW);
            GL11.glCullFace(GL11.GL_BACK);

            GL11.glPushMatrix();
            GL11.glTranslatef(screenX, screenY, 0f);
            GL11.glRotatef(spec.getTilt(), 0f, 0f, 1f);
            GL11.glRotatef(spec.getPitch(), 1f, 0f, 0f);
            GL11.glRotatef(rotationAngle, 0f, 1f, 0f);
            GL11.glRotatef(-90f, 1f, 0f, 0f);
            discTexture.bindTexture();

            Misc.setColor(discColor, alphaMult);
            unitSphere.draw(discRadius);
            Misc.setColor(discColor, alphaMult * RIM_ALPHA_MULT);
            unitSphere.draw(discRadius + 0.25f * zoom);
            unitSphere.draw(discRadius + 0.5f * zoom);

            GL11.glPopMatrix();
        }
    }

    private void renderAtmosphere(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        if (atmosphereTexture == null) {
            atmosphereTexture = Global.getSettings().getSprite(AtmosphereRing.TEXTURE_CATEGORY, AtmosphereRing.TEXTURE_ID);
        }
        SpriteAPI texture = atmosphereTexture;
        if (texture == null) return;

        for (Star star : stars) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            if (spec == null || spec.getAtmosphereThickness() <= 0f) {
                continue;
            }

            float starRadius = radiusOf(star) * zoom;
            float thickness = Math.max(radiusOf(star) * spec.getAtmosphereThickness(), spec.getAtmosphereThicknessMin()) * zoom;
            if (thickness > 0f) {
                float innerRadius = starRadius - thickness * AtmosphereRing.INNER_INSET_MULT;
                float outerRadius = innerRadius + thickness;

                float screenX = viewport.screenX(star.getX());
                float screenY = viewport.screenY(star.getY());
                if (viewport.isVisible(screenX, screenY, outerRadius)) {
                    AtmosphereRing.draw(texture, screenX, screenY, innerRadius, outerRadius, resolveColor(star, spec.getAtmosphereColor()),
                            alphaOf(star, alphaMult));
                }
            }
        }
    }

    private void renderAurora(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        if (auroraTexture == null) {
            auroraTexture = Global.getSettings().getSprite(StarAurora.TEXTURE_CATEGORY, StarAurora.TEXTURE_ID);
        }
        SpriteAPI texture = auroraTexture;
        if (texture == null) return;

        for (Star star : stars) {
            renderStarAurora(star, viewport, zoom, alphaOf(star, alphaMult), texture);
        }
    }

    private void renderStarAurora(Star star, TreeViewport viewport, float zoom, float alphaMult, SpriteAPI texture) {
        PlanetSpecAPI spec = resolveSpec(star.getStarType());
        if (spec == null) return;

        float detailRadius = star.getRadius() * SmoothZoom.MAX_ZOOM;
        float screenScale = zoom * radiusOf(star) / star.getRadius() / SmoothZoom.MAX_ZOOM;

        float screenX = viewport.screenX(star.getX());
        float screenY = viewport.screenY(star.getY());
        if (!viewport.isVisible(screenX, screenY, StarAurora.reach(detailRadius) * screenScale)) {
            return;
        }
        animationOf(star).aurora.render(texture, screenX, screenY, detailRadius, screenScale, resolveColor(star, spec.getCoronaColor()), alphaMult);
    }

    private StarAnimation animationOf(Star star) {
        return animationById.computeIfAbsent(star.getId(), id -> new StarAnimation());
    }

    private void renderGlow(TreeViewport viewport, float alphaMult) {
        float zoom = viewport.zoom();
        for (Star star : stars) {
            PlanetSpecAPI spec = resolveSpec(star.getStarType());
            String coronaPath = spec == null ? null : spec.getCoronaTexture();
            if (coronaPath == null || coronaPath.isEmpty()) {
                continue;
            }

            float starRadius = radiusOf(star) * zoom;
            float haloRadius = radiusOf(star) * spec.getCoronaSize() * zoom;
            float screenX = viewport.screenX(star.getX());
            float screenY = viewport.screenY(star.getY());
            if (haloRadius > starRadius && viewport.isVisible(screenX, screenY, haloRadius)) {
                SpriteDraw.drawAdditiveAtCenter(spriteCache, coronaPath, screenX, screenY,
                        haloRadius * 2f, haloRadius * 2f, resolveColor(star, spec.getCoronaColor()), alphaOf(star, alphaMult));
            }
        }
    }

    private Color resolveColor(Star star, Color fallback) {
        Map<Color, Color> byFallback = resolvedColors.computeIfAbsent(star.getId(), id -> new HashMap<>());
        return byFallback.computeIfAbsent(fallback, base -> {
            Color parsed = ColorUtil.parseHexColor(star.getColor(), base);
            return new Color(parsed.getRed(), parsed.getGreen(), parsed.getBlue(), base.getAlpha());
        });
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

    private static final class StarAnimation {
        private final StarAurora aurora = new StarAurora();
        private float rotationAngle;
    }
}
