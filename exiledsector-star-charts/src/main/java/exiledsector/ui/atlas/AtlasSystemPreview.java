package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.decoration.PlanetSphere;
import exiledsector.PlanetSpecs;
import exiledsector.skills.skilleffect.render.CategorySprite;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.layout.sketch.ChartSystemSketch;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Belt;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Body;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Moon;
import exiledsector.charts.layout.sketch.ChartSystemSketch.PlanetRing;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.decoration.AtmosphereRing;
import exiledsector.ui.decoration.StarAurora;
import exiledsector.ui.decoration.UnitSphere;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AtlasSystemPreview {

    record Prepared(ChartSystemSketch sketch, float outerRadius, List<String> starKeys, AtlasSystemArt.Layout art) {

        static Prepared of(String key, ChartSystemSketch sketch) {
            return of(key, sketch, List.of());
        }

        static Prepared of(String key, ChartSystemSketch sketch, List<String> anomalyIds) {
            List<String> starKeys = new ArrayList<>();
            for (int i = 0; i < sketch.stars().size(); i++) {
                starKeys.add(key + "_" + i);
            }
            return new Prepared(sketch, Math.max(MIN_OUTER_RADIUS, sketch.outerRadius()), List.copyOf(starKeys),
                    AtlasSystemArt.layout(key.hashCode(), sketch, anomalyIds));
        }
    }

    enum Layer {
        BELT(5f), PHOTOSPHERE(4f), NEBULA(3f), STAR(2f), PLANET(1f), PLANET_RING(0f), MOON(-1f);

        final float untiltedDepth;

        Layer(float untiltedDepth) {
            this.untiltedDepth = untiltedDepth;
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
    static final float RIM_ALPHA_MULT = 0.37f;
    static final float PHOTOSPHERE_GLOW_SHARE = 0.5f;
    static final float PHOTOSPHERE_GLOW_ALPHA = 0.55f;
    static final float PHOTOSPHERE_HEART_SHARE = 0.14f;
    static final float PHOTOSPHERE_CORE_SHARE = 0.035f;
    static final float PHOTOSPHERE_CORE_WHITENING = 0.6f;
    static final float PHOTOSPHERE_SPOTLIGHT_SHARE = 3f;
    static final float AURORA_DETAIL_RADIUS = 400f;
    static final float MIN_BAND_PIXELS = 2f;
    static final float SATELLITE_MIN_PARENT_PIXELS = 4f;
    static final float SATELLITE_SPREAD = 0.6f;
    static final float RING_SLOT_WIDTH = 256f;
    static final float SHADOW_FLOOR = 0.25f;
    static final float SHADOW_SLOPE = 0.9f;
    static final float SHADOW_MAX = 0.9f;
    static final float DETAILED_SPHERE_PIXELS = 24f;
    static final int DETAILED_SPHERE_DETAIL = 72;
    static final int MIN_DISC_SEGMENTS = 16;
    static final int MAX_DISC_SEGMENTS = 128;
    static final float PIXELS_PER_DISC_SEGMENT = 3f;
    static final int BAND_DEPTH_ARCS = 12;
    static final Color FALLBACK_STAR_COLOUR = new Color(255, 240, 210);
    static final Color FALLBACK_PLANET_COLOUR = new Color(170, 170, 180);
    static final String ASTEROID_RINGS = "graphics/planets/rings_asteroids0.png";
    static final String DUST_RINGS = "graphics/planets/rings_dust0.png";
    static final String ICE_RINGS = "graphics/planets/rings_ice0.png";
    static final String SPECIAL_RINGS = "graphics/planets/rings_special0.png";
    private static final UnitCircle GLOW_CIRCLE = UnitCircle.of(64);
    private static final RingBeltRenderer.Arc[] WHOLE_BAND = {RingBeltRenderer.WHOLE_RING};
    private static final RingBeltRenderer.Arc[] BAND_ARCS = bandArcs(BAND_DEPTH_ARCS);

    private final SpriteCache spriteCache = new SpriteCache(AtlasSystemPreview.class);
    private final UnitSphere unitSphere = new UnitSphere();
    private final UnitSphere detailedSphere = new UnitSphere(DETAILED_SPHERE_DETAIL);
    private final AtlasSystemArt art = new AtlasSystemArt();
    private final AtlasSystemArt.Frame frame = new AtlasSystemArt.Frame();
    private final Map<String, StarAurora> auroras = new HashMap<>();
    private final Vector2f bandCentre = new Vector2f();
    private final CategorySprite auroraTexture = new CategorySprite(StarAurora.TEXTURE_CATEGORY, StarAurora.TEXTURE_ID);
    private final CategorySprite atmosphereTexture = new CategorySprite(AtmosphereRing.TEXTURE_CATEGORY, AtmosphereRing.TEXTURE_ID);
    private final OrbitalPlaneCamera camera = new OrbitalPlaneCamera();
    private final float[] planeMatrixValues = new float[OrbitalPlaneCamera.MATRIX_VALUES];
    private final FloatBuffer planeMatrix = BufferUtils.createFloatBuffer(OrbitalPlaneCamera.MATRIX_VALUES);
    private final DepthOrder<Layer> drawOrder = new DepthOrder<>();
    private final Spots stars = new Spots();
    private final Spots planets = new Spots();
    private final Spots moons = new Spots();
    private RingBeltRenderer.Arc[] bandArcs = WHOLE_BAND;
    private float centreScreenX;
    private float centreScreenY;
    private float orbitX;
    private float orbitY;

    void advance(float amount) {
        for (StarAurora aurora : auroras.values()) {
            aurora.advance(amount);
        }
    }

    void render(Prepared prepared, float cx, float cy, float radius, float elapsedSeconds, float alpha) {
        render(prepared, cx, cy, radius, prepared.outerRadius(), elapsedSeconds, alpha);
    }

    void render(Prepared prepared, float cx, float cy, float radius, float outerRadius, float elapsedSeconds, float alpha) {
        render(prepared, cx, cy, radius, outerRadius, elapsedSeconds, alpha, Spotlight.NONE, 0f);
    }

    void render(Prepared prepared, float cx, float cy, float radius, float outerRadius, float elapsedSeconds, float alpha, Spotlight spotlight,
                float tiltLevel) {
        if (radius <= 0f || alpha <= 0f || outerRadius <= 0f) {
            return;
        }
        ChartSystemSketch sketch = prepared.sketch();
        Fit fit = new Fit(cx, cy, radius, radius * ORBIT_REACH_SHARE, outerRadius, elapsedSeconds);
        aimCamera(fit, tiltLevel);
        float bodyAlpha = alpha * (1f - spotlight.level());
        startFrame(fit, sketch.planets().size(), bodyAlpha);
        placeStars(sketch, fit, spotlight);
        placePlanets(sketch, fit);
        placeMoons(sketch);
        queueScene(prepared, fit, bodyAlpha);
        for (int rank = 0; rank < drawOrder.size(); rank++) {
            drawItem(prepared, drawOrder.kind(rank), drawOrder.index(rank), drawOrder.part(rank), fit, alpha, bodyAlpha, spotlight);
        }
        if (bodyAlpha > 0f) {
            art.renderAnomalies(prepared.art(), frame);
        }
    }

    record Spotlight(float level, float starPixels) {

        static final Spotlight NONE = new Spotlight(0f, 0f);
    }

    private static final class Spots {
        float[] planeX = new float[0];
        float[] planeY = new float[0];
        float[] planePixels = new float[0];
        float[] screenX = new float[0];
        float[] screenY = new float[0];
        float[] screenPixels = new float[0];
        boolean[] shown = new boolean[0];

        void ensure(int count) {
            if (planeX.length < count) {
                planeX = new float[count];
                planeY = new float[count];
                planePixels = new float[count];
                screenX = new float[count];
                screenY = new float[count];
                screenPixels = new float[count];
                shown = new boolean[count];
            }
        }

        void place(int index, OrbitalPlaneCamera camera, float x, float y, float pixels) {
            planeX[index] = x;
            planeY[index] = y;
            planePixels[index] = pixels;
            screenX[index] = camera.screenX(x, y);
            screenY[index] = camera.screenY(y);
            screenPixels[index] = pixels * camera.sizeAt(y);
            shown[index] = true;
        }
    }

    private void aimCamera(Fit fit, float tiltLevel) {
        camera.aim(fit.cx(), fit.cy(), fit.reach(), tiltLevel);
        camera.writePlaneMatrix(planeMatrixValues);
        for (int index = 0; index < planeMatrixValues.length; index++) {
            planeMatrix.put(index, planeMatrixValues[index]);
        }
        bandArcs = camera.tilted() ? BAND_ARCS : WHOLE_BAND;
        centreScreenX = camera.screenX(0f, 0f);
        centreScreenY = camera.screenY(0f);
    }

    private void startFrame(Fit fit, int planetCount, float alpha) {
        frame.radius = fit.radius();
        frame.reach = fit.reach();
        frame.starX = centreScreenX;
        frame.starY = centreScreenY;
        frame.starPixels = fit.radius() * MIN_STAR_SHARE;
        frame.starColour = FALLBACK_STAR_COLOUR;
        frame.elapsedSeconds = fit.elapsedSeconds();
        frame.alpha = alpha;
        frame.ensurePlanets(planetCount);
    }

    private record Fit(float cx, float cy, float radius, float reach, float outer, float elapsedSeconds) {

        float scaled(float orbitRadius) {
            return reach * (float) Math.sqrt(Math.max(0f, orbitRadius) / outer);
        }
    }

    static RingBeltRenderer.Arc[] bandArcs(int count) {
        RingBeltRenderer.Arc[] arcs = new RingBeltRenderer.Arc[count];
        for (int index = 0; index < count; index++) {
            arcs[index] = new RingBeltRenderer.Arc(index, count);
        }
        return arcs;
    }

    static float arcDepth(float centreDepth, float bandRadius, RingBeltRenderer.Arc arc) {
        return centreDepth + bandRadius * (float) Math.sin(Math.toRadians(arc.middleDegrees()));
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

    static int discSegments(float pixels) {
        int segments = Math.round((float) (2 * Math.PI) * pixels / PIXELS_PER_DISC_SEGMENT);
        return Math.max(MIN_DISC_SEGMENTS, Math.min(MAX_DISC_SEGMENTS, segments));
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

    private void orbit(Body body, Fit fit) {
        double angle = Math.toRadians(angleAt(body.startAngle(), body.orbitDays(), fit.elapsedSeconds()));
        float distance = fit.scaled(body.orbitRadius());
        orbitX = (float) Math.cos(angle) * distance;
        orbitY = (float) Math.sin(angle) * distance;
    }

    private void placeStars(ChartSystemSketch sketch, Fit fit, Spotlight spotlight) {
        stars.ensure(sketch.stars().size());
        for (int index = 0; index < sketch.stars().size(); index++) {
            Body star = sketch.stars().get(index);
            orbit(star, fit);
            stars.place(index, camera, orbitX, orbitY, starPixels(star, fit.radius()));
            PlanetSpecAPI spec = spec(star.typeId());
            if (index == 0 && (spec == null || !spec.isNebulaCenter())) {
                spotlightPrimary(spotlight, fit);
                frame.starX = stars.screenX[0];
                frame.starY = stars.screenY[0];
                frame.starPixels = stars.screenPixels[0];
                frame.starColour = corona(spec);
            }
        }
    }

    private void spotlightPrimary(Spotlight spotlight, Fit fit) {
        if (spotlight.level() <= 0f) {
            return;
        }
        stars.screenX[0] = AtlasMapCanvas.lerp(stars.screenX[0], fit.cx(), spotlight.level());
        stars.screenY[0] = AtlasMapCanvas.lerp(stars.screenY[0], fit.cy(), spotlight.level());
        if (spotlight.starPixels() > 0f) {
            stars.screenPixels[0] = AtlasMapCanvas.lerp(stars.screenPixels[0], spotlight.starPixels(), spotlight.level());
        }
    }

    private static Color corona(PlanetSpecAPI spec) {
        return spec == null || spec.getCoronaColor() == null ? FALLBACK_STAR_COLOUR : spec.getCoronaColor();
    }

    private void placePlanets(ChartSystemSketch sketch, Fit fit) {
        planets.ensure(sketch.planets().size());
        for (int index = 0; index < sketch.planets().size(); index++) {
            Body planet = sketch.planets().get(index);
            orbit(planet, fit);
            float pixels = planet.orbitRadius() <= 0f ? starPixels(planet, fit.radius()) : planetPixels(planet, fit.radius());
            planets.place(index, camera, orbitX, orbitY, pixels);
            frame.planetX[index] = planets.screenX[index];
            frame.planetY[index] = planets.screenY[index];
            frame.planetPixels[index] = planets.screenPixels[index];
        }
    }

    private void placeMoons(ChartSystemSketch sketch) {
        moons.ensure(sketch.moons().size());
        for (int index = 0; index < sketch.moons().size(); index++) {
            Moon moon = sketch.moons().get(index);
            int parent = moon.planet();
            moons.shown[index] = showsSatellites(sketch, parent);
            if (moons.shown[index]) {
                Body parentBody = sketch.planets().get(parent);
                float parentPixels = planets.planePixels[parent];
                double angle = Math.toRadians(angleAt(moon.body().startAngle(), moon.body().orbitDays(), frame.elapsedSeconds));
                float distance = satelliteDistance(moon.body().orbitRadius(), parentBody.radius(), parentPixels);
                moons.place(index, camera, planets.planeX[parent] + (float) Math.cos(angle) * distance,
                        planets.planeY[parent] + (float) Math.sin(angle) * distance, moonPixels(moon.body(), parentBody, parentPixels));
            }
        }
    }

    private float depth(Layer layer, float planeY) {
        return camera.tilted() ? planeY : layer.untiltedDepth;
    }

    private void queueScene(Prepared prepared, Fit fit, float bodyAlpha) {
        ChartSystemSketch sketch = prepared.sketch();
        drawOrder.clear();
        if (bodyAlpha > 0f) {
            for (int index = 0; index < sketch.belts().size(); index++) {
                queueBand(Layer.BELT, index, 0f, fit.scaled(sketch.belts().get(index).orbitRadius()));
            }
        }
        if (sketch.photosphere() != null) {
            drawOrder.add(Layer.PHOTOSPHERE, 0, 0, depth(Layer.PHOTOSPHERE, 0f));
        }
        if (prepared.art().nebulaSheet() != null) {
            drawOrder.add(Layer.NEBULA, 0, 0, camera.tilted() ? Float.MAX_VALUE : Layer.NEBULA.untiltedDepth);
        }
        for (int index = 0; index < sketch.stars().size(); index++) {
            drawOrder.add(Layer.STAR, index, 0, depth(Layer.STAR, stars.planeY[index]));
        }
        if (bodyAlpha <= 0f) {
            drawOrder.sortBackToFront();
            return;
        }
        for (int index = 0; index < sketch.planets().size(); index++) {
            drawOrder.add(Layer.PLANET, index, 0, depth(Layer.PLANET, planets.planeY[index]));
        }
        for (int index = 0; index < sketch.planetRings().size(); index++) {
            PlanetRing ring = sketch.planetRings().get(index);
            int parent = ring.planet();
            if (showsSatellites(sketch, parent)) {
                float ringRadius = satelliteDistance(ring.belt().orbitRadius(), sketch.planets().get(parent).radius(), planets.planePixels[parent]);
                queueBand(Layer.PLANET_RING, index, planets.planeY[parent], ringRadius);
            }
        }
        for (int index = 0; index < sketch.moons().size(); index++) {
            if (moons.shown[index]) {
                drawOrder.add(Layer.MOON, index, 0, depth(Layer.MOON, moons.planeY[index]));
            }
        }
        drawOrder.sortBackToFront();
    }

    private void queueBand(Layer layer, int index, float centreDepth, float bandRadius) {
        for (int arc = 0; arc < bandArcs.length; arc++) {
            drawOrder.add(layer, index, arc, camera.tilted() ? arcDepth(centreDepth, bandRadius, bandArcs[arc]) : layer.untiltedDepth);
        }
    }

    private void drawItem(Prepared prepared, Layer layer, int index, int part, Fit fit, float alpha, float bodyAlpha, Spotlight spotlight) {
        ChartSystemSketch sketch = prepared.sketch();
        switch (layer) {
            case BELT -> drawBelt(sketch.belts().get(index), index, bandArcs[part], fit, bodyAlpha);
            case PHOTOSPHERE -> drawPhotosphere(sketch.photosphere(), fit, spotlight, alpha);
            case NEBULA -> art.renderNebula(prepared.art(), frame);
            case STAR -> drawStar(prepared.starKeys().get(index), sketch.stars().get(index), index, fit, index == 0 ? alpha : bodyAlpha);
            case PLANET -> drawWorld(sketch.planets().get(index), planets.screenX[index], planets.screenY[index], planets.screenPixels[index], fit,
                    bodyAlpha);
            case PLANET_RING -> drawPlanetRing(sketch, sketch.planetRings().get(index), bandArcs[part], fit, bodyAlpha);
            case MOON -> drawWorld(sketch.moons().get(index).body(), moons.screenX[index], moons.screenY[index], moons.screenPixels[index], fit,
                    bodyAlpha);
            default -> {
            }
        }
    }

    private void drawBelt(Belt belt, int index, RingBeltRenderer.Arc arc, Fit fit, float alpha) {
        float inner = fit.scaled(belt.orbitRadius() - belt.width() / 2f);
        float outer = Math.max(inner + MIN_BAND_PIXELS, fit.scaled(belt.orbitRadius() + belt.width() / 2f));
        drawBand(belt, belt.variant() + index, 0f, 0f, inner, outer, arc, fit, alpha);
    }

    private void drawPlanetRing(ChartSystemSketch sketch, PlanetRing ring, RingBeltRenderer.Arc arc, Fit fit, float alpha) {
        int parent = ring.planet();
        Belt band = ring.belt();
        float parentRadius = sketch.planets().get(parent).radius();
        float parentPixels = planets.planePixels[parent];
        float inner = satelliteDistance(band.orbitRadius() - band.width() / 2f, parentRadius, parentPixels);
        float outer = Math.max(inner + MIN_BAND_PIXELS, satelliteDistance(band.orbitRadius() + band.width() / 2f, parentRadius, parentPixels));
        drawBand(band, band.variant(), planets.planeX[parent], planets.planeY[parent], inner, outer, arc, fit, alpha);
    }

    private void drawBand(Belt band, int variant, float planeCentreX, float planeCentreY, float inner, float outer, RingBeltRenderer.Arc arc, Fit fit,
                          float alpha) {
        SpriteAPI texture = spriteCache.texture(ringTexture(band.typeId()));
        if (texture == null || texture.getWidth() <= 0f) {
            return;
        }
        int slots = Math.max(1, Math.round(texture.getWidth() / RING_SLOT_WIDTH));
        bandCentre.set(planeCentreX, planeCentreY);
        GL11.glPushMatrix();
        GL11.glMultMatrix(planeMatrix);
        RingBeltRenderer.renderSlotArc(texture, new RadialBand(bandCentre, inner, outer), variant % slots, RING_SLOT_WIDTH, Color.WHITE, alpha,
                angleAt(0f, band.orbitDays(), fit.elapsedSeconds()), arc);
        GL11.glPopMatrix();
    }

    private void drawStar(String auroraKey, Body star, int index, Fit fit, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        PlanetSpecAPI spec = spec(star.typeId());
        if (spec != null && spec.isNebulaCenter()) {
            return;
        }
        Color corona = corona(spec);
        float x = stars.screenX[index];
        float y = stars.screenY[index];
        float pixels = stars.screenPixels[index];
        String coronaPath = spec == null ? null : spec.getCoronaTexture();
        if (coronaPath != null && !coronaPath.isEmpty()) {
            float halo = Math.min(fit.radius() * MAX_CORONA_SHARE, pixels * Math.max(1f, spec.getCoronaSize()));
            SpriteDraw.drawAdditiveAtCenter(spriteCache, coronaPath, x, y, halo * 2f, halo * 2f, corona, alpha);
        }
        SpriteAPI auroraSprite = auroraTexture.get();
        if (auroraSprite != null && pixels >= AURORA_MIN_PIXELS) {
            StarAurora aurora = auroras.computeIfAbsent(auroraKey, id -> new StarAurora());
            aurora.render(auroraSprite, x, y, AURORA_DETAIL_RADIUS, pixels / AURORA_DETAIL_RADIUS, corona, alpha);
        }
        drawBody(spec, x, y, pixels, star.startAngle(), fit.elapsedSeconds(), FALLBACK_STAR_COLOUR, alpha, true);
    }

    private void drawPhotosphere(Color colour, Fit fit, Spotlight spotlight, float alpha) {
        float x = AtlasMapCanvas.lerp(centreScreenX, fit.cx(), spotlight.level());
        float y = AtlasMapCanvas.lerp(centreScreenY, fit.cy(), spotlight.level());
        float radius = fit.radius() * camera.sizeAt(0f);
        if (spotlight.starPixels() > 0f) {
            radius = AtlasMapCanvas.lerp(radius, spotlight.starPixels() * PHOTOSPHERE_SPOTLIGHT_SHARE, spotlight.level());
        }
        try (GlScope glow = GlScope.flat().blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            HoloShapes.centreGlow(x, y, radius * PHOTOSPHERE_GLOW_SHARE, GLOW_CIRCLE, colour, PHOTOSPHERE_GLOW_ALPHA * alpha);
            HoloShapes.centreGlow(x, y, radius * PHOTOSPHERE_HEART_SHARE, GLOW_CIRCLE, colour, alpha);
        }
        try (GlScope flat = GlScope.flat()) {
            float coreRadius = Math.max(MIN_BODY_PIXELS, radius * PHOTOSPHERE_CORE_SHARE);
            HoloShapes.disc(x, y, coreRadius, discSegments(coreRadius), whitened(colour), alpha);
        }
    }

    static Color whitened(Color colour) {
        return new Color(whiten(colour.getRed()), whiten(colour.getGreen()), whiten(colour.getBlue()));
    }

    private static int whiten(int channel) {
        return Math.round(channel + (255 - channel) * PHOTOSPHERE_CORE_WHITENING);
    }

    private boolean showsSatellites(ChartSystemSketch sketch, int parent) {
        return parent >= 0 && parent < sketch.planets().size() && frame.planetPixels[parent] >= SATELLITE_MIN_PARENT_PIXELS;
    }

    static float satelliteDistance(float orbitRadius, float parentRadius, float parentPixels) {
        float clearance = Math.max(0f, orbitRadius - parentRadius) / Math.max(1f, parentRadius);
        return parentPixels * (1f + SATELLITE_SPREAD * (float) Math.sqrt(clearance));
    }

    static float moonPixels(Body moon, Body parent, float parentPixels) {
        return Math.max(MIN_BODY_PIXELS, parentPixels * (float) Math.sqrt(moon.radius() / Math.max(1f, parent.radius())));
    }

    private void drawWorld(Body world, float x, float y, float pixels, Fit fit, float alpha) {
        PlanetSpecAPI spec = spec(world.typeId());
        drawBody(spec, x, y, pixels, world.startAngle(), fit.elapsedSeconds(), FALLBACK_PLANET_COLOUR, alpha, false);
        float dx = centreScreenX - x;
        float dy = centreScreenY - y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0f && pixels >= SHADOW_MIN_PIXELS) {
            try (GlScope flat = GlScope.flat()) {
                drawShadow(x, y, pixels, dx / length, dy / length, alpha);
            }
        }
        SpriteAPI atmosphere = atmosphereTexture.get();
        if (spec != null && atmosphere != null && pixels >= ATMOSPHERE_MIN_PIXELS && spec.getAtmosphereThickness() > 0f
                && spec.getAtmosphereColor() != null) {
            float thickness = pixels * spec.getAtmosphereThickness();
            float inner = pixels - thickness * AtmosphereRing.INNER_INSET_MULT;
            AtmosphereRing.draw(atmosphere, x, y, inner, inner + thickness, spec.getAtmosphereColor(), alpha);
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
                HoloShapes.disc(x, y, pixels, discSegments(pixels), flatColour, alpha);
            }
            return;
        }
        float rotation = startAngle + spec.getRotation() * elapsedSeconds;
        UnitSphere sphere = pixels >= DETAILED_SPHERE_PIXELS ? detailedSphere : unitSphere;
        PlanetSphere.draw(sphere, texture, spec, x, y, pixels, rotation, colour, alpha, rim ? alpha * RIM_ALPHA_MULT : 0f, pixels * 0.03f);
    }

    private static void drawShadow(float x, float y, float radius, float lightX, float lightY, float alpha) {
        UnitCircle circle = UnitCircle.of(discSegments(radius));
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(0f, 0f, 0f, SHADOW_FLOOR * alpha);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= circle.segments(); i++) {
            int index = i % circle.segments();
            float cos = circle.cos(index);
            float sin = circle.sin(index);
            GL11.glColor4f(0f, 0f, 0f, shadowAlpha(cos, sin, lightX, lightY) * alpha);
            GL11.glVertex2f(x + cos * radius, y + sin * radius);
        }
        GL11.glEnd();
    }

    private static PlanetSpecAPI spec(String typeId) {
        return PlanetSpecs.byType(typeId);
    }
}
