package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.decoration.ThreatUnit;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.List;

public final class ThreatUnitRenderer implements DecorationRenderer {

    static final String FABRICATOR_SPRITE_PATH = "graphics/ships/threat/fabricator.png";
    static final String HIVE_SPRITE_PATH = "graphics/ships/threat/threat_hive.png";
    static final String FRAGMENT_SPRITE_PATH = "graphics/ships/threat/fragment_single.png";
    static final String FRAGMENT_GLOW_PATH = "graphics/fx/hit_glow.png";
    static final int FABRICATOR_FRAGMENTS = 40;
    static final int HIVE_FRAGMENTS = 64;
    private static final Color THREAT_GLOW = new Color(130, 155, 145);
    private static final float SWAY_DEGREES = 4f;
    private static final float SWAY_SECONDS = 23f;
    private static final float DRIFT_LENGTHS = 0.03f;
    private static final float DRIFT_SECONDS = 31f;
    private static final float SWARM_INNER_LENGTHS = 0.55f;
    private static final float SWARM_DEPTH_LENGTHS = 0.45f;
    private static final float FRAGMENT_LENGTHS = 0.05f;
    private static final float GLOW_SIZE_MULT = 3f;
    private static final float GLOW_ALPHA = 0.35f;
    private static final float MIN_ORBIT_SECONDS = 9f;
    private static final float EXTRA_ORBIT_SECONDS = 14f;
    private static final float WOBBLE_LENGTHS = 0.06f;
    private static final float GOLDEN_FRACTION = 0.618034f;
    private static final float TWO_PI = (float) (Math.PI * 2);

    private final SpriteCache spriteCache = new SpriteCache(ThreatUnitRenderer.class);
    private final TexturedQuadBatch fragmentQuads = new TexturedQuadBatch();
    private final List<ThreatUnit> units;
    private float elapsedSeconds;

    ThreatUnitRenderer(List<ThreatUnit> units) {
        this.units = List.copyOf(units);
    }

    @Override
    public void advance(float amount) {
        if (!units.isEmpty()) {
            elapsedSeconds += amount;
        }
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        if (depth != DecorationDepth.UNITS || units.isEmpty() || scene.sceneryAlpha() <= 0f) {
            return;
        }
        for (ThreatUnit unit : units) {
            float unitAlpha = scene.alphaFor(unit.getId());
            if (unitAlpha > 0f) {
                renderUnit(scene.viewport(), unit, unitAlpha);
            }
        }
    }

    static String spritePath(ThreatUnit unit) {
        return ThreatUnit.HIVE.equals(unit.getUnitType()) ? HIVE_SPRITE_PATH : FABRICATOR_SPRITE_PATH;
    }

    static int fragmentCount(ThreatUnit unit) {
        return ThreatUnit.HIVE.equals(unit.getUnitType()) ? HIVE_FRAGMENTS : FABRICATOR_FRAGMENTS;
    }

    private void renderUnit(TreeViewport viewport, ThreatUnit unit, float alphaMult) {
        float zoom = viewport.zoom();
        float hullLength = unit.getLength();
        float driftPhase = TWO_PI * elapsedSeconds / DRIFT_SECONDS;
        float unitX = unit.getX() + (float) Math.cos(driftPhase) * hullLength * DRIFT_LENGTHS;
        float unitY = unit.getY() + (float) Math.sin(driftPhase * 2f) * hullLength * DRIFT_LENGTHS;
        float screenX = viewport.screenX(unitX);
        float screenY = viewport.screenY(unitY);
        float swarmReach = hullLength * (SWARM_INNER_LENGTHS + SWARM_DEPTH_LENGTHS + WOBBLE_LENGTHS);
        if (!viewport.isVisible(screenX, screenY, swarmReach * zoom)) {
            return;
        }
        renderSwarm(viewport, unit, unitX, unitY, alphaMult, false);
        SpriteAPI hull = spriteCache.sprite(spritePath(unit));
        if (hull != null) {
            float aspect = hull.getHeight() <= 0f ? 1f : hull.getWidth() / hull.getHeight();
            float swayDeg = SWAY_DEGREES * (float) Math.sin(TWO_PI * elapsedSeconds / SWAY_SECONDS);
            SpriteDraw.drawAtCenter(hull, screenX, screenY, hullLength * aspect * zoom, hullLength * zoom, Color.WHITE, alphaMult,
                    -(unit.getRotation() + swayDeg));
        }
        renderSwarm(viewport, unit, unitX, unitY, alphaMult, true);
    }

    private void renderSwarm(TreeViewport viewport, ThreatUnit unit, float unitX, float unitY, float alphaMult, boolean inFront) {
        SpriteAPI fragmentTexture = spriteCache.texture(FRAGMENT_SPRITE_PATH);
        SpriteAPI glowTexture = spriteCache.texture(FRAGMENT_GLOW_PATH);
        if (fragmentTexture == null || glowTexture == null) {
            return;
        }
        float zoom = viewport.zoom();
        float fragmentSize = unit.getLength() * FRAGMENT_LENGTHS * zoom;
        int count = fragmentCount(unit);
        try (TexturedQuadBatch glows = fragmentQuads.beginAdditive(glowTexture)) {
            for (int index = 0; index < count; index++) {
                if (isInFront(index, elapsedSeconds) == inFront) {
                    glows.quadAtCenter(viewport.screenX(fragmentX(unit, unitX, index)), viewport.screenY(fragmentY(unit, unitY, index)),
                            fragmentSize * GLOW_SIZE_MULT, fragmentSize * GLOW_SIZE_MULT, 0f, THREAT_GLOW, alphaMult * GLOW_ALPHA);
                }
            }
        }
        try (TexturedQuadBatch fragments = fragmentQuads.beginNormal(fragmentTexture)) {
            for (int index = 0; index < count; index++) {
                if (isInFront(index, elapsedSeconds) == inFront) {
                    fragments.quadAtCenter(viewport.screenX(fragmentX(unit, unitX, index)), viewport.screenY(fragmentY(unit, unitY, index)),
                            fragmentSize, fragmentSize, spinDegrees(index, elapsedSeconds), Color.WHITE, alphaMult);
                }
            }
        }
    }

    static float orbitAngle(int index, float atSeconds) {
        float seed = fraction(index * GOLDEN_FRACTION);
        float direction = index % 3 == 0 ? -1f : 1f;
        float orbitSeconds = MIN_ORBIT_SECONDS + EXTRA_ORBIT_SECONDS * fraction(seed * 7.31f);
        return seed * TWO_PI + direction * TWO_PI * atSeconds / orbitSeconds;
    }

    static boolean isInFront(int index, float atSeconds) {
        return Math.sin(orbitAngle(index, atSeconds)) > 0;
    }

    private float fragmentX(ThreatUnit unit, float unitX, int index) {
        return unitX + (float) Math.cos(orbitAngle(index, elapsedSeconds)) * orbitRadius(unit, index);
    }

    private float fragmentY(ThreatUnit unit, float unitY, int index) {
        float tilt = 0.45f + 0.25f * fraction(index * 0.414214f);
        return unitY + (float) Math.sin(orbitAngle(index, elapsedSeconds)) * orbitRadius(unit, index) * tilt;
    }

    private float orbitRadius(ThreatUnit unit, int index) {
        float hullLength = unit.getLength();
        float depth = fraction(index * 0.754878f);
        float wobble = (float) Math.sin(elapsedSeconds * (0.7f + depth) + index) * WOBBLE_LENGTHS;
        return hullLength * (SWARM_INNER_LENGTHS + SWARM_DEPTH_LENGTHS * depth + wobble);
    }

    private static float spinDegrees(int index, float atSeconds) {
        return fraction(index * 0.569840f) * 360f + atSeconds * (40f + 60f * fraction(index * 0.324718f));
    }

    private static float fraction(float value) {
        return value - (float) Math.floor(value);
    }
}
