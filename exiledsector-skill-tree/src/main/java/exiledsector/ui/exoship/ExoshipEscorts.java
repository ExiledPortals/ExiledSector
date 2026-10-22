package exiledsector.ui.exoship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.compat.RandomAssortmentCompat;
import exiledlib.skills.HullSizeTable;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.HullEngines;
import exiledlib.skills.skilleffect.Fade;
import exiledlib.skills.skilleffect.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;
import exiledsector.ui.util.WobblyOrbit;

import java.util.Arrays;

final class ExoshipEscorts {

    record Escort(String spritePath, String hullId, HullSize hullSize, float width, float height, float lengthShare, float orbitShare,
                  float secondsPerLap, float direction, float startRad, WobblyOrbit orbit) {
    }

    record Pose(float treeX, float treeY, float facingDeg, boolean inFront) {
    }

    static final Escort[] ESCORTS = {
            escort("graphics/fx/rat_hypatia.png", "rat_hypatia", HullSize.DESTROYER, 108f, 160f, 0.04f, 0.85f, 0.5f, 24f, 1f, 0f, 20f, 0.06f, 3),
            escort("graphics/fx/rat_dawnblade.png", "rat_dawnblade", HullSize.FIGHTER, 60f, 60f, 0.035f, 0.62f, 0.55f, 13f, -1f, 2f, -35f, 0.1f, 4),
            escort("graphics/fx/rat_nightblade.png", "rat_nightblade", HullSize.FIGHTER, 60f, 60f, 0.035f, 0.72f, 0.45f, 15f, 1f, 4f, 60f, 0.08f, 5),
            escort("graphics/fx/rat_protostar.png", "rat_protostar", HullSize.FIGHTER, 60f, 60f, 0.03f, 0.56f, 0.6f, 11f, -1f, 1f, 110f, 0.12f, 3)
    };
    static final int MAX_ESCORTS = ESCORTS.length + 1;
    static final float BEHIND_ALPHA = 0.7f;
    private static final float ESCORT_ENGINE_LEVEL = 0.8f;
    private static final float TANGENT_STEP_RAD = 0.002f;

    static final float RECALL_SECONDS = 2.4f;
    private static final float LAUNCH_STAGGER = 0.12f;
    static final float DEPART_GAP_SECONDS = 1.08f;
    private static final float RECALL_GAP_SECONDS = RECALL_SECONDS * LAUNCH_STAGGER;
    private static final float RECALL_TRAVEL_SECONDS = RECALL_SECONDS - RECALL_GAP_SECONDS * (MAX_ESCORTS - 1);
    private static final float ORDER_SETTLED_SECONDS = DEPART_GAP_SECONDS * MAX_ESCORTS;
    static final float HANGAR_LEVEL = 0.35f;
    private static final float HANGAR_SCALE = 0.55f;
    private static final float VANISH_LEVEL = 0.15f;
    private static final HullSizeTable PLAYER_LENGTH_SHARE = new HullSizeTable(0.04f, 0.025f, 0.04f, 0.06f, 0.08f, 0.04f);
    private static final HullSizeTable DEPART_SECONDS = new HullSizeTable(2f, 2.5f, 3.1f, 3.9f, 4.68f, 3.1f);
    private static final WobblyOrbit.Point ORBIT_POINT = new WobblyOrbit.Point();
    private static final WobblyOrbit.Point AHEAD_POINT = new WobblyOrbit.Point();

    private final SpriteCache spriteCache = new SpriteCache(ExoshipEscorts.class);
    private final TexturedQuadBatch glowQuads = new TexturedQuadBatch();
    private final HullEngines[] escortEngines = new HullEngines[ESCORTS.length];
    private Escort playerEscort;
    private HullEngines playerEngines = HullEngines.NONE;
    private float elapsedSeconds;
    private final float[] escortLevels = initialEscortLevels();
    private float secondsSinceOrder = ORDER_SETTLED_SECONDS;
    private boolean returning;

    private static float[] initialEscortLevels() {
        float[] levels = new float[MAX_ESCORTS];
        Arrays.fill(levels, 1f);
        return levels;
    }

    private static Escort escort(String spritePath, String hullId, HullSize hullSize, float width, float height, float lengthShare, float orbitShare,
                                 float squash, float secondsPerLap, float direction, float startRad, float tiltDeg, float wobbleShare,
                                 int wobbleWaves) {
        return new Escort(spritePath, hullId, hullSize, width, height, lengthShare, orbitShare, secondsPerLap, direction, startRad,
                new WobblyOrbit(wobbleShare, wobbleWaves, 0f, squash, tiltDeg));
    }

    static Escort playerEscort(String spritePath, String hullId, float spriteWidth, float spriteHeight, HullSize hullSize) {
        return escort(spritePath, hullId, hullSize, spriteWidth, spriteHeight, playerLengthShare(hullSize), 0.66f, 0.5f, 17f, 1f, 3f, -70f, 0.07f, 4);
    }

    static float playerLengthShare(HullSize hullSize) {
        return PLAYER_LENGTH_SHARE.of(hullSize);
    }

    void setPlayerEscort(Escort escort, HullEngines engines) {
        playerEscort = escort;
        playerEngines = engines == null ? HullEngines.NONE : engines;
        if (RandomAssortmentCompat.isModEnabled()) {
            for (int escortIndex = 0; escortIndex < ESCORTS.length; escortIndex++) {
                engines(escortIndex);
            }
        }
    }

    int escortCount() {
        return playerEscort == null ? ESCORTS.length : ESCORTS.length + 1;
    }

    Escort escort(int escortIndex) {
        return escortIndex < ESCORTS.length ? ESCORTS[escortIndex] : playerEscort;
    }

    static float departSeconds(HullSize hullSize) {
        return DEPART_SECONDS.of(hullSize);
    }

    void advance(float amount, boolean launched) {
        elapsedSeconds += amount;
        if (returning == launched) {
            returning = !launched;
            secondsSinceOrder = 0f;
        }
        secondsSinceOrder = Math.min(ORDER_SETTLED_SECONDS, secondsSinceOrder + amount);
        for (int escortIndex = 0; escortIndex < MAX_ESCORTS; escortIndex++) {
            Escort escort = escort(escortIndex);
            float departSeconds = departSeconds(escort == null ? null : escort.hullSize());
            escortLevels[escortIndex] = stepEscort(escortLevels[escortIndex], escortIndex, departSeconds, launched, secondsSinceOrder, amount);
        }
    }

    static float stepEscort(float escortLevel, int escortIndex, float departSeconds, boolean launched, float secondsSinceOrder,
                            float amount) {
        if (launched) {
            boolean released = secondsSinceOrder >= DEPART_GAP_SECONDS * escortIndex;
            return released ? Fade.step(escortLevel, true, amount / departSeconds) : escortLevel;
        }
        boolean recalled = secondsSinceOrder >= RECALL_GAP_SECONDS * (MAX_ESCORTS - 1 - escortIndex);
        return recalled ? Fade.step(escortLevel, false, amount / RECALL_TRAVEL_SECONDS) : escortLevel;
    }

    float elapsedSeconds() {
        return elapsedSeconds;
    }

    float escortLevel(int escortIndex) {
        return escortLevels[escortIndex];
    }

    static Pose travelPose(Escort escort, float centerX, float centerY, float shipLength, float atSeconds, float escortLevel,
                           boolean returning) {
        Pose orbit = pose(escort, centerX, centerY, shipLength, atSeconds);
        if (escortLevel >= 1f) {
            return orbit;
        }
        float eased = Fade.smooth(escortLevel);
        float offsetX = orbit.treeX() - centerX;
        float offsetY = orbit.treeY() - centerY;
        double outwardRad = Math.atan2(offsetY, offsetX);
        double travelRad = returning ? outwardRad + Math.PI : outwardRad;
        double orbitRad = Math.toRadians(orbit.facingDeg());
        float facingX = (float) (Math.cos(orbitRad) * eased + Math.cos(travelRad) * (1f - eased));
        float facingY = (float) (Math.sin(orbitRad) * eased + Math.sin(travelRad) * (1f - eased));
        float facingDeg = (float) Math.toDegrees(Math.atan2(facingY, facingX));
        return new Pose(centerX + offsetX * eased, centerY + offsetY * eased, facingDeg, orbit.inFront() && escortLevel > HANGAR_LEVEL);
    }

    static Pose pose(Escort escort, float centerX, float centerY, float shipLength, float atSeconds) {
        float angle = escort.startRad() + escort.direction() * (float) (Math.PI * 2) * atSeconds / escort.secondsPerLap();
        float orbitRadius = escort.orbitShare() * shipLength;
        WobblyOrbit.Point point = escort.orbit().locate(centerX, centerY, orbitRadius, angle, ORBIT_POINT);
        float x = point.x();
        float y = point.y();
        float ahead = angle + escort.direction() * TANGENT_STEP_RAD;
        WobblyOrbit.Point aheadPoint = escort.orbit().locate(centerX, centerY, orbitRadius, ahead, AHEAD_POINT);
        float facingDeg = (float) Math.toDegrees(Math.atan2(aheadPoint.y() - y, aheadPoint.x() - x));
        return new Pose(x, y, facingDeg, Math.sin(angle) > 0);
    }

    void render(TreeViewport viewport, float centerX, float centerY, float shipLength, boolean inFront, float alphaMult) {
        if (alphaMult <= 0f) {
            return;
        }
        float zoom = viewport.zoom();
        for (int escortIndex = 0; escortIndex < escortCount(); escortIndex++) {
            Escort escort = escort(escortIndex);
            float escortLevel = escortLevels[escortIndex];
            if (escortLevel <= 0f) {
                continue;
            }
            Pose pose = travelPose(escort, centerX, centerY, shipLength, elapsedSeconds, escortLevel, returning);
            if (pose.inFront() != inFront) {
                continue;
            }
            float scale = HANGAR_SCALE + (1f - HANGAR_SCALE) * Fade.smooth(escortLevel);
            float length = escort.lengthShare() * shipLength * zoom * scale;
            float width = length * escort.width() / escort.height();
            float screenX = viewport.screenX(pose.treeX());
            float screenY = viewport.screenY(pose.treeY());
            if (!viewport.isVisible(screenX, screenY, length)) {
                continue;
            }
            float alpha = alphaMult * (inFront ? 1f : BEHIND_ALPHA) * Math.min(1f, escortLevel / VANISH_LEVEL);
            float screenFacingDeg = -pose.facingDeg();
            float screenScale = length / escort.height();
            HullEngines engines = engines(escortIndex);
            engines.drawHull(spriteCache, escort.spritePath(), screenX, screenY, width, length, screenScale, screenFacingDeg, alpha);
            engines.drawEngines(spriteCache, glowQuads, screenX, screenY, screenFacingDeg, screenScale, ESCORT_ENGINE_LEVEL, alpha);
        }
    }

    private HullEngines engines(int escortIndex) {
        if (escortIndex >= ESCORTS.length) {
            return playerEngines;
        }
        if (escortEngines[escortIndex] == null) {
            escortEngines[escortIndex] = enginesFor(ESCORTS[escortIndex].hullId());
        }
        return escortEngines[escortIndex];
    }

    private static HullEngines enginesFor(String hullId) {
        try {
            return HullEngines.of(Global.getSettings().getHullSpec(hullId));
        } catch (RuntimeException e) {
            return HullEngines.NONE;
        }
    }
}
