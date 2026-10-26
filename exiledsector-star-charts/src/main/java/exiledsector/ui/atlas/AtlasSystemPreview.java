package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.ChartSystemSketch;
import exiledsector.charts.ChartSystemSketch.Belt;
import exiledsector.charts.ChartSystemSketch.Body;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.decoration.AtmosphereRing;
import exiledsector.ui.decoration.StarAurora;
import exiledsector.ui.decoration.UnitSphere;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AtlasSystemPreview {

    record Prepared(ChartSystemSketch sketch, float outerRadius, List<String> starKeys) {

        static Prepared of(String key, ChartSystemSketch sketch) {
            List<String> starKeys = new ArrayList<>();
            for (int i = 0; i < sketch.stars().size(); i++) {
                starKeys.add(key + "_" + i);
            }
            return new Prepared(sketch, Math.max(MIN_OUTER_RADIUS, sketch.outerRadius()), List.copyOf(starKeys));
        }
    }

    static final float MIN_OUTER_RADIUS = 1500f;
    static final float SECONDS_PER_DAY = 0.25f;
    static final float STAR_SHARE = 0.12f;
    static final float STAR_REFERENCE_RADIUS = 700f;
    static final float MIN_STAR_SHARE = 0.05f;
    static final float MAX_STAR_SHARE = 0.16f;
    static final float PLANET_SHARE = 0.04f;
    static final float PLANET_REFERENCE_RADIUS = 150f;
    static final float MIN_PLANET_SHARE = 0.022f;
    static final float MAX_PLANET_SHARE = 0.07f;
    static final float MIN_BODY_PIXELS = 1f;
    static final float SPHERE_MIN_PIXELS = 2.5f;
    static final float AURORA_MIN_PIXELS = 5f;
    static final float ATMOSPHERE_MIN_PIXELS = 4f;
    static final float MAX_CORONA_SHARE = 0.3f;
    static final float ORBIT_REACH_SHARE = 0.82f;
    static final float SHADOW_MIN_PIXELS = 3f;
    static final float NEBULA_SHARE = 0.55f;
    static final float NEBULA_ALPHA = 0.5f;
    static final float RIM_ALPHA_MULT = 0.37f;
    static final float AURORA_DETAIL_RADIUS = 400f;
    static final float MIN_BAND_PIXELS = 2f;
    static final float RING_SLOT_WIDTH = 256f;
    static final float SHADOW_FLOOR = 0.25f;
    static final float SHADOW_SLOPE = 0.9f;
    static final float SHADOW_MAX = 0.9f;
    static final Color FALLBACK_STAR_COLOUR = new Color(255, 240, 210);
    static final Color FALLBACK_PLANET_COLOUR = new Color(170, 170, 180);
    static final String ASTEROID_RINGS = "graphics/planets/rings_asteroids0.png";
    static final String DUST_RINGS = "graphics/planets/rings_dust0.png";
    static final String ICE_RINGS = "graphics/planets/rings_ice0.png";
    static final String SPECIAL_RINGS = "graphics/planets/rings_special0.png";
    private static final UnitCircle GLOW_CIRCLE = UnitCircle.of(64);
    private static final UnitCircle DISC_CIRCLE = UnitCircle.of(16);

    private final SpriteCache spriteCache = new SpriteCache(AtlasSystemPreview.class);
    private final UnitSphere unitSphere = new UnitSphere();
    private final Map<String, PlanetSpecAPI> specs = new HashMap<>();
    private final Map<String, StarAurora> auroras = new HashMap<>();
    private final Vector2f bandCentre = new Vector2f();
    private SpriteAPI auroraTexture;
    private SpriteAPI atmosphereTexture;
    private boolean specsLoaded;
    private boolean sharedTexturesLoaded;
    private float bodyX;
    private float bodyY;

    void advance(float amount) {
        for (StarAurora aurora : auroras.values()) {
            aurora.advance(amount);
        }
    }

    void render(Prepared prepared, float cx, float cy, float radius, float elapsedSeconds, float alpha) {
        if (radius <= 0f || alpha <= 0f) {
            return;
        }
        loadSharedTextures();
        ChartSystemSketch sketch = prepared.sketch();
        Fit fit = new Fit(cx, cy, radius, radius * ORBIT_REACH_SHARE, prepared.outerRadius(), elapsedSeconds);
        for (int i = 0; i < sketch.belts().size(); i++) {
            drawBelt(sketch.belts().get(i), i, fit, alpha);
        }
        for (int i = 0; i < sketch.stars().size(); i++) {
            drawStar(prepared.starKeys().get(i), sketch.stars().get(i), fit, alpha);
        }
        for (Body planet : sketch.planets()) {
            drawPlanet(planet, fit, alpha);
        }
    }

    private record Fit(float cx, float cy, float radius, float reach, float outer, float elapsedSeconds) {

        float scaled(float orbitRadius) {
            return reach * (float) Math.sqrt(Math.max(0f, orbitRadius) / outer);
        }
    }

    static float angleAt(float startAngle, float orbitDays, float elapsedSeconds) {
        return orbitDays <= 0f ? startAngle : startAngle + 360f * elapsedSeconds / (orbitDays * SECONDS_PER_DAY);
    }

    static float starPixels(Body star, float radius) {
        float share = STAR_SHARE * star.radius() / STAR_REFERENCE_RADIUS;
        return Math.max(MIN_BODY_PIXELS, radius * Math.max(MIN_STAR_SHARE, Math.min(MAX_STAR_SHARE, share)));
    }

    static float planetPixels(Body planet, float radius) {
        float share = PLANET_SHARE * planet.radius() / PLANET_REFERENCE_RADIUS;
        return Math.max(MIN_BODY_PIXELS, radius * Math.max(MIN_PLANET_SHARE, Math.min(MAX_PLANET_SHARE, share)));
    }

    static String ringTexture(String typeId) {
        if (typeId == null) {
            return DUST_RINGS;
        }
        if (typeId.contains("asteroid")) {
            return ASTEROID_RINGS;
        }
        if (typeId.contains("ice")) {
            return ICE_RINGS;
        }
        return typeId.contains("special") ? SPECIAL_RINGS : DUST_RINGS;
    }

    static float shadowAlpha(float normalX, float normalY, float lightX, float lightY) {
        float facing = normalX * lightX + normalY * lightY;
        return Math.max(0f, Math.min(SHADOW_MAX, SHADOW_FLOOR - facing * SHADOW_SLOPE));
    }

    private void place(Body body, Fit fit) {
        double angle = Math.toRadians(angleAt(body.startAngle(), body.orbitDays(), fit.elapsedSeconds()));
        float distance = fit.scaled(body.orbitRadius());
        bodyX = fit.cx() + (float) Math.cos(angle) * distance;
        bodyY = fit.cy() + (float) Math.sin(angle) * distance;
    }

    private void drawBelt(Belt belt, int index, Fit fit, float alpha) {
        SpriteAPI texture = spriteCache.texture(ringTexture(belt.typeId()));
        if (texture == null || texture.getWidth() <= 0f) {
            return;
        }
        float inner = fit.scaled(belt.orbitRadius() - belt.width() / 2f);
        float outer = Math.max(inner + MIN_BAND_PIXELS, fit.scaled(belt.orbitRadius() + belt.width() / 2f));
        int slots = Math.max(1, Math.round(texture.getWidth() / RING_SLOT_WIDTH));
        bandCentre.set(fit.cx(), fit.cy());
        RingBeltRenderer.renderSlot(texture, new RadialBand(bandCentre, inner, outer), (belt.variant() + index) % slots, RING_SLOT_WIDTH,
                Color.WHITE, alpha, angleAt(0f, belt.orbitDays(), fit.elapsedSeconds()));
    }

    private void drawStar(String auroraKey, Body star, Fit fit, float alpha) {
        PlanetSpecAPI spec = spec(star.typeId());
        place(star, fit);
        Color corona = spec == null || spec.getCoronaColor() == null ? FALLBACK_STAR_COLOUR : spec.getCoronaColor();
        if (spec != null && spec.isNebulaCenter()) {
            try (GlScope glow = GlScope.flat().blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
                drawGlow(bodyX, bodyY, fit.radius() * NEBULA_SHARE, corona, NEBULA_ALPHA * alpha);
            }
            return;
        }
        float pixels = starPixels(star, fit.radius());
        String coronaPath = spec == null ? null : spec.getCoronaTexture();
        if (coronaPath != null && !coronaPath.isEmpty()) {
            float halo = Math.min(fit.radius() * MAX_CORONA_SHARE, pixels * Math.max(1f, spec.getCoronaSize()));
            SpriteDraw.drawAdditiveAtCenter(spriteCache, coronaPath, bodyX, bodyY, halo * 2f, halo * 2f, corona, alpha);
        }
        if (auroraTexture != null && pixels >= AURORA_MIN_PIXELS) {
            StarAurora aurora = auroras.computeIfAbsent(auroraKey, id -> new StarAurora());
            aurora.render(auroraTexture, bodyX, bodyY, AURORA_DETAIL_RADIUS, pixels / AURORA_DETAIL_RADIUS, corona, alpha);
        }
        drawBody(spec, bodyX, bodyY, pixels, star.startAngle(), fit.elapsedSeconds(), FALLBACK_STAR_COLOUR, alpha, true);
    }

    private void drawPlanet(Body planet, Fit fit, float alpha) {
        PlanetSpecAPI spec = spec(planet.typeId());
        place(planet, fit);
        float pixels = planet.orbitRadius() <= 0f ? starPixels(planet, fit.radius()) : planetPixels(planet, fit.radius());
        drawBody(spec, bodyX, bodyY, pixels, planet.startAngle(), fit.elapsedSeconds(), FALLBACK_PLANET_COLOUR, alpha, false);
        float dx = fit.cx() - bodyX;
        float dy = fit.cy() - bodyY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0f && pixels >= SHADOW_MIN_PIXELS) {
            try (GlScope flat = GlScope.flat()) {
                drawShadow(bodyX, bodyY, pixels, dx / length, dy / length, alpha);
            }
        }
        if (spec != null && atmosphereTexture != null && pixels >= ATMOSPHERE_MIN_PIXELS && spec.getAtmosphereThickness() > 0f
                && spec.getAtmosphereColor() != null) {
            float thickness = pixels * spec.getAtmosphereThickness();
            float inner = pixels - thickness * AtmosphereRing.INNER_INSET_MULT;
            AtmosphereRing.draw(atmosphereTexture, bodyX, bodyY, inner, inner + thickness, spec.getAtmosphereColor(), alpha);
        }
    }

    private void drawBody(PlanetSpecAPI spec, float x, float y, float pixels, float startAngle, float elapsedSeconds, Color fallback, float alpha,
                          boolean rim) {
        String texturePath = spec == null ? null : spec.getTexture();
        SpriteAPI texture = pixels < SPHERE_MIN_PIXELS || texturePath == null || texturePath.isEmpty() ? null : spriteCache.texture(texturePath);
        Color colour = spec != null && spec.getPlanetColor() != null ? spec.getPlanetColor() : fallback;
        if (texture == null) {
            Color flatColour = spec != null && spec.getIconColor() != null ? spec.getIconColor() : fallback;
            try (GlScope flat = GlScope.flat()) {
                drawDisc(x, y, pixels, flatColour, alpha);
            }
            return;
        }
        float rotation = startAngle + spec.getRotation() * elapsedSeconds;
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS | GL11.GL_POLYGON_BIT).enable(GL11.GL_TEXTURE_2D).normalBlend()
                .enable(GL11.GL_CULL_FACE)) {
            GL11.glFrontFace(GL11.GL_CW);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glRotatef(spec.getTilt(), 0f, 0f, 1f);
            GL11.glRotatef(spec.getPitch(), 1f, 0f, 0f);
            GL11.glRotatef(rotation, 0f, 1f, 0f);
            GL11.glRotatef(-90f, 1f, 0f, 0f);
            texture.bindTexture();
            Misc.setColor(colour, alpha);
            unitSphere.draw(pixels);
            if (rim) {
                Misc.setColor(colour, alpha * RIM_ALPHA_MULT);
                unitSphere.draw(pixels * 1.03f);
                unitSphere.draw(pixels * 1.06f);
            }
            GL11.glPopMatrix();
        }
    }

    private static void drawDisc(float x, float y, float radius, Color colour, float alpha) {
        Misc.setColor(colour, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= DISC_CIRCLE.segments(); i++) {
            int index = i % DISC_CIRCLE.segments();
            GL11.glVertex2f(x + DISC_CIRCLE.cos(index) * radius, y + DISC_CIRCLE.sin(index) * radius);
        }
        GL11.glEnd();
    }

    private static void drawShadow(float x, float y, float radius, float lightX, float lightY, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(0f, 0f, 0f, SHADOW_FLOOR * alpha);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= DISC_CIRCLE.segments(); i++) {
            int index = i % DISC_CIRCLE.segments();
            float cos = DISC_CIRCLE.cos(index);
            float sin = DISC_CIRCLE.sin(index);
            GL11.glColor4f(0f, 0f, 0f, shadowAlpha(cos, sin, lightX, lightY) * alpha);
            GL11.glVertex2f(x + cos * radius, y + sin * radius);
        }
        GL11.glEnd();
    }

    private static void drawGlow(float x, float y, float radius, Color colour, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        Misc.setColor(colour, alpha);
        GL11.glVertex2f(x, y);
        Misc.setColor(colour, 0f);
        for (int i = 0; i <= GLOW_CIRCLE.segments(); i++) {
            int index = i % GLOW_CIRCLE.segments();
            GL11.glVertex2f(x + GLOW_CIRCLE.cos(index) * radius, y + GLOW_CIRCLE.sin(index) * radius);
        }
        GL11.glEnd();
    }

    private void loadSharedTextures() {
        if (sharedTexturesLoaded) {
            return;
        }
        sharedTexturesLoaded = true;
        SettingsAPI settings = Global.getSettings();
        if (settings != null) {
            auroraTexture = settings.getSprite(StarAurora.TEXTURE_CATEGORY, StarAurora.TEXTURE_ID);
            atmosphereTexture = settings.getSprite(AtmosphereRing.TEXTURE_CATEGORY, AtmosphereRing.TEXTURE_ID);
        }
    }

    private PlanetSpecAPI spec(String typeId) {
        if (!specsLoaded) {
            specsLoaded = true;
            SettingsAPI settings = Global.getSettings();
            if (settings != null) {
                for (PlanetSpecAPI spec : settings.getAllPlanetSpecs()) {
                    specs.put(spec.getPlanetType(), spec);
                }
            }
        }
        return specs.get(typeId);
    }
}
