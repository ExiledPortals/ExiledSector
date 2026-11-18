package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.charts.tree.AtlasOrbitLayout;
import exiledsector.charts.tree.AtlasOrbitSystem;
import exiledsector.charts.tree.AtlasOrbitSystem.Track;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.util.TexturedQuadBatch;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class AtlasBeltPainter {

    private static final float HALF_WIDTH = 75f;
    private static final String ASTEROID_BANDS_PATH = "graphics/planets/rings_asteroids0.png";
    private static final String ICE_BANDS_PATH = "graphics/planets/rings_ice0.png";
    private static final String[] ASTEROID_PATHS = {"graphics/asteroids/asteroid1.png", "graphics/asteroids/asteroid2.png",
            "graphics/asteroids/asteroid3.png", "graphics/asteroids/asteroid4.png"};
    private static final float[] ASTEROID_SPRITE_RADII = {5f, 10f, 15f};
    private static final float BAND_SLOT_WIDTH = 256f;
    private static final int INNER_BAND_SLOTS = 2;
    private static final int OUTER_BAND_SLOTS = 4;
    private static final float BAND_WIDTH = 100f;
    private static final float BAND_OFFSET = HALF_WIDTH - BAND_WIDTH / 2f;
    private static final float INNER_BAND_SPEED = 1f / 1.05f;
    private static final float BAND_ALPHA = 0.9f;
    private static final float WORLD_UNITS_PER_ASTEROID = 450f;
    private static final float ASTEROID_SPREAD = 0.9f;
    private static final float MIN_ASTEROID_RADIUS = 3f;
    private static final float MAX_ASTEROID_RADIUS = 12f;
    private static final float MIN_ASTEROID_SPEED = 0.67f;
    private static final float MAX_ASTEROID_SPEED = 1.33f;
    private static final float MAX_ASTEROID_SPIN = 5f;
    private static final float MIN_ASTEROID_PIXELS = 1f;
    private static final long SEED = 20261118L;

    private record Belt(Track track, boolean ice, int innerSlot, int outerSlot) {
    }

    private record Asteroid(Track track, float orbitRadius, float startDegrees, float speedShare, float radius, float facingDegrees,
                            float spinDegreesPerSecond, int sprite) {
    }

    private final SpriteCache sprites = new SpriteCache(AtlasBeltPainter.class);
    private final TexturedQuadBatch batch = new TexturedQuadBatch();
    private final List<Belt> belts = new ArrayList<>();
    private final List<Asteroid> asteroids = new ArrayList<>();
    private final Vector2f bandCentre = new Vector2f();

    AtlasBeltPainter(List<AtlasOrbitLayout.Belt> layoutBelts) {
        Random random = new Random(SEED);
        for (AtlasOrbitLayout.Belt layoutBelt : layoutBelts) {
            Track track = layoutBelt.track();
            int innerSlot = random.nextInt(INNER_BAND_SLOTS);
            int outerSlot = (innerSlot + 1 + random.nextInt(OUTER_BAND_SLOTS - 1)) % OUTER_BAND_SLOTS;
            belts.add(new Belt(track, layoutBelt.outermost(), innerSlot, outerSlot));
            if (!layoutBelt.outermost()) {
                scatterAsteroids(track, random);
            }
        }
        asteroids.sort(Comparator.comparingInt(Asteroid::sprite));
    }

    private void scatterAsteroids(Track track, Random random) {
        int count = Math.round((float) (2 * Math.PI * track.radius()) / WORLD_UNITS_PER_ASTEROID);
        for (int index = 0; index < count; index++) {
            float radius = MIN_ASTEROID_RADIUS + random.nextFloat() * (MAX_ASTEROID_RADIUS - MIN_ASTEROID_RADIUS);
            float orbitRadius = track.radius() + (random.nextFloat() * 2f - 1f) * HALF_WIDTH * ASTEROID_SPREAD;
            float speedShare = MIN_ASTEROID_SPEED + random.nextFloat() * (MAX_ASTEROID_SPEED - MIN_ASTEROID_SPEED);
            asteroids.add(new Asteroid(track, orbitRadius, random.nextFloat() * 360f, speedShare, radius, random.nextFloat() * 360f,
                    (random.nextFloat() * 2f - 1f) * MAX_ASTEROID_SPIN, spriteFor(radius)));
        }
    }

    static int spriteFor(float radius) {
        for (int sprite = 0; sprite < ASTEROID_SPRITE_RADII.length; sprite++) {
            if (radius < ASTEROID_SPRITE_RADII[sprite]) {
                return sprite;
            }
        }
        return ASTEROID_SPRITE_RADII.length;
    }

    void render(TreeViewport viewport, AtlasOrbitSystem.Positions positions, double orbitSeconds, float alpha) {
        for (Belt belt : belts) {
            renderBands(viewport, positions, belt, orbitSeconds, alpha);
        }
        renderAsteroids(viewport, positions, orbitSeconds, alpha);
    }

    private void renderBands(TreeViewport viewport, AtlasOrbitSystem.Positions positions, Belt belt, double orbitSeconds, float alpha) {
        Track track = belt.track();
        float zoom = viewport.zoom();
        float centreX = viewport.screenX(positions.x(track.centreIndex()));
        float centreY = viewport.screenY(positions.y(track.centreIndex()));
        float outer = (track.radius() + HALF_WIDTH) * zoom;
        if (!viewport.overlaps(centreX - outer, centreY - outer, centreX + outer, centreY + outer)) {
            return;
        }
        SpriteAPI texture = sprites.texture(belt.ice() ? ICE_BANDS_PATH : ASTEROID_BANDS_PATH);
        if (texture == null) {
            return;
        }
        bandCentre.set(centreX, centreY);
        float turn = turnDegrees(track, 1f, orbitSeconds);
        float innerTurn = turnDegrees(track, INNER_BAND_SPEED, orbitSeconds);
        drawBand(texture, belt.innerSlot(), track.radius() - BAND_OFFSET, -innerTurn, viewport, alpha);
        drawBand(texture, belt.outerSlot(), track.radius() + BAND_OFFSET, -turn, viewport, alpha);
    }

    private void drawBand(SpriteAPI texture, int slot, float middleRadius, float rotationDegrees, TreeViewport viewport, float alpha) {
        float zoom = viewport.zoom();
        RadialBand band = new RadialBand(bandCentre, (middleRadius - BAND_WIDTH / 2f) * zoom, (middleRadius + BAND_WIDTH / 2f) * zoom);
        RingBeltRenderer.renderSlotTrueScale(texture, band, slot, BAND_SLOT_WIDTH, Color.WHITE, alpha * BAND_ALPHA, rotationDegrees, viewport);
    }

    private void renderAsteroids(TreeViewport viewport, AtlasOrbitSystem.Positions positions, double orbitSeconds, float alpha) {
        float zoom = viewport.zoom();
        if (MAX_ASTEROID_RADIUS * 2f * zoom < MIN_ASTEROID_PIXELS) {
            return;
        }
        int index = 0;
        while (index < asteroids.size()) {
            int sprite = asteroids.get(index).sprite();
            int end = index;
            while (end < asteroids.size() && asteroids.get(end).sprite() == sprite) {
                end++;
            }
            SpriteAPI texture = sprites.sprite(ASTEROID_PATHS[sprite]);
            if (texture != null) {
                try (TexturedQuadBatch quads = batch.beginNormal(texture)) {
                    for (int member = index; member < end; member++) {
                        drawAsteroid(quads, asteroids.get(member), viewport, positions, orbitSeconds, alpha);
                    }
                }
            }
            index = end;
        }
    }

    private static void drawAsteroid(TexturedQuadBatch quads, Asteroid asteroid, TreeViewport viewport, AtlasOrbitSystem.Positions positions,
                                     double orbitSeconds, float alpha) {
        float size = asteroid.radius() * 2f * viewport.zoom();
        if (size < MIN_ASTEROID_PIXELS) {
            return;
        }
        Track track = asteroid.track();
        double angle = Math.toRadians(asteroid.startDegrees() + turnDegrees(track, asteroid.speedShare(), orbitSeconds));
        float screenX = viewport.screenX(positions.x(track.centreIndex()) + (float) Math.cos(angle) * asteroid.orbitRadius());
        float screenY = viewport.screenY(positions.y(track.centreIndex()) + (float) Math.sin(angle) * asteroid.orbitRadius());
        if (!viewport.overlaps(screenX - size, screenY - size, screenX + size, screenY + size)) {
            return;
        }
        float facing = (float) ((asteroid.facingDegrees() + asteroid.spinDegreesPerSecond() * orbitSeconds) % 360.0);
        quads.quadAtCenter(screenX, screenY, size, size, facing, Color.WHITE, alpha);
    }

    private static float turnDegrees(Track track, float speedShare, double orbitSeconds) {
        return (float) (track.degreesPerSecond() * speedShare * orbitSeconds % 360.0);
    }
}
