package exiledsector.ui.exoship;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.Fade;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

final class ExoshipEscorts {

    record Escort(String spritePath, float width, float height, float lengthShare, float orbitShare, float squash, float secondsPerLap,
                  float direction, float startRad, float tiltDeg, float wobbleShare, int wobbleWaves) {
    }

    record Pose(float treeX, float treeY, float facingDeg, boolean inFront) {
    }

    static final Escort[] ESCORTS = {
            new Escort("graphics/fx/rat_hypatia.png", 108f, 160f, 0.04f, 0.85f, 0.5f, 24f, 1f, 0f, 20f, 0.06f, 3),
            new Escort("graphics/fx/rat_dawnblade.png", 60f, 60f, 0.035f, 0.62f, 0.55f, 13f, -1f, 2f, -35f, 0.1f, 4),
            new Escort("graphics/fx/rat_nightblade.png", 60f, 60f, 0.035f, 0.72f, 0.45f, 15f, 1f, 4f, 60f, 0.08f, 5),
            new Escort("graphics/fx/rat_protostar.png", 60f, 60f, 0.03f, 0.56f, 0.6f, 11f, -1f, 1f, 110f, 0.12f, 3)
    };
    static final int MAX_ESCORTS = ESCORTS.length + 1;
    static final float BEHIND_ALPHA = 0.7f;
    private static final float NOZZLE_SHARE = 0.45f;
    private static final float FLAME_LENGTH_SHARE = 0.5f;
    private static final float FLAME_WIDTH_SHARE = 0.18f;
    private static final Color ENGINE_COLOR = new Color(247, 121, 86);
    private static final float TANGENT_STEP_RAD = 0.002f;

    static final float LAUNCH_SECONDS = 2.4f;
    static final float LAUNCH_STAGGER = 0.12f;
    static final float HANGAR_LEVEL = 0.35f;
    private static final float HANGAR_SCALE = 0.55f;
    private static final float VANISH_LEVEL = 0.15f;

    private final SpriteCache spriteCache = new SpriteCache(ExoshipEscorts.class);
    private Escort playerEscort;
    private float elapsedSeconds;
    private float launchLevel = 1f;
    private boolean returning;

    static Escort playerEscort(String spritePath, float spriteWidth, float spriteHeight, HullSize hullSize) {
        return new Escort(spritePath, spriteWidth, spriteHeight, playerLengthShare(hullSize), 0.66f, 0.5f, 17f, 1f, 3f, -70f, 0.07f, 4);
    }

    static float playerLengthShare(HullSize hullSize) {
        if (hullSize == null) {
            return 0.04f;
        }
        return switch (hullSize) {
            case FRIGATE -> 0.025f;
            case CRUISER -> 0.06f;
            case CAPITAL_SHIP -> 0.08f;
            default -> 0.04f;
        };
    }

    void setPlayerEscort(Escort escort) {
        playerEscort = escort;
    }

    int escortCount() {
        return playerEscort == null ? ESCORTS.length : ESCORTS.length + 1;
    }

    Escort escort(int escortIndex) {
        return escortIndex < ESCORTS.length ? ESCORTS[escortIndex] : playerEscort;
    }

    void advance(float amount, boolean launched) {
        elapsedSeconds += amount;
        returning = !launched;
        launchLevel = Fade.step(launchLevel, launched, amount / LAUNCH_SECONDS);
    }

    float elapsedSeconds() {
        return elapsedSeconds;
    }

    float launchLevel() {
        return launchLevel;
    }

    static float escortLevel(float launchLevel, int escortIndex) {
        float span = 1f - LAUNCH_STAGGER * (MAX_ESCORTS - 1);
        return Math.max(0f, Math.min(1f, (launchLevel - LAUNCH_STAGGER * escortIndex) / span));
    }

    static Pose travelPose(Escort escort, float centerX, float centerY, float shipLength, float atSeconds, float escortLevel,
                           boolean returning) {
        Pose orbit = pose(escort, centerX, centerY, shipLength, atSeconds);
        if (escortLevel >= 1f) {
            return orbit;
        }
        float eased = smooth(escortLevel);
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

    private static float smooth(float level) {
        return level * level * (3f - 2f * level);
    }

    static Pose pose(Escort escort, float centerX, float centerY, float shipLength, float atSeconds) {
        float angle = escort.startRad() + escort.direction() * (float) (Math.PI * 2) * atSeconds / escort.secondsPerLap();
        float x = loopX(escort, centerX, centerY, shipLength, angle);
        float y = loopY(escort, centerX, centerY, shipLength, angle);
        float ahead = angle + escort.direction() * TANGENT_STEP_RAD;
        float facingDeg = (float) Math.toDegrees(Math.atan2(loopY(escort, centerX, centerY, shipLength, ahead) - y,
                loopX(escort, centerX, centerY, shipLength, ahead) - x));
        return new Pose(x, y, facingDeg, Math.sin(angle) > 0);
    }

    private static float radius(Escort escort, float shipLength, float angle) {
        return escort.orbitShare() * shipLength * (1f + escort.wobbleShare() * (float) Math.sin(escort.wobbleWaves() * angle));
    }

    private static float loopX(Escort escort, float centerX, float centerY, float shipLength, float angle) {
        float radius = radius(escort, shipLength, angle);
        float localX = radius * (float) Math.cos(angle);
        float localY = radius * escort.squash() * (float) Math.sin(angle);
        double tilt = Math.toRadians(escort.tiltDeg());
        return centerX + localX * (float) Math.cos(tilt) - localY * (float) Math.sin(tilt);
    }

    private static float loopY(Escort escort, float centerX, float centerY, float shipLength, float angle) {
        float radius = radius(escort, shipLength, angle);
        float localX = radius * (float) Math.cos(angle);
        float localY = radius * escort.squash() * (float) Math.sin(angle);
        double tilt = Math.toRadians(escort.tiltDeg());
        return centerY + localX * (float) Math.sin(tilt) + localY * (float) Math.cos(tilt);
    }

    void render(TreeViewport viewport, float centerX, float centerY, float shipLength, boolean inFront, float alphaMult) {
        if (alphaMult <= 0f) {
            return;
        }
        float zoom = viewport.zoom();
        for (int escortIndex = 0; escortIndex < escortCount(); escortIndex++) {
            Escort escort = escort(escortIndex);
            float escortLevel = escortLevel(launchLevel, escortIndex);
            if (escortLevel <= 0f) {
                continue;
            }
            Pose pose = travelPose(escort, centerX, centerY, shipLength, elapsedSeconds, escortLevel, returning);
            if (pose.inFront() != inFront) {
                continue;
            }
            float scale = HANGAR_SCALE + (1f - HANGAR_SCALE) * smooth(escortLevel);
            float length = escort.lengthShare() * shipLength * zoom * scale;
            float width = length * escort.width() / escort.height();
            float screenX = viewport.screenX(pose.treeX());
            float screenY = viewport.screenY(pose.treeY());
            if (!viewport.isVisible(screenX, screenY, length)) {
                continue;
            }
            float alpha = alphaMult * (inFront ? 1f : BEHIND_ALPHA) * Math.min(1f, escortLevel / VANISH_LEVEL);
            float angle = -pose.facingDeg() - 90f;
            double screenFacing = Math.toRadians(-pose.facingDeg());
            float forwardX = (float) Math.cos(screenFacing);
            float forwardY = (float) Math.sin(screenFacing);
            float nozzleX = screenX - forwardX * length * NOZZLE_SHARE;
            float nozzleY = screenY - forwardY * length * NOZZLE_SHARE;
            float flicker = EngineFlames.flicker(elapsedSeconds, escortIndex);
            EngineFlames.draw(spriteCache, nozzleX, nozzleY, forwardX, forwardY, pose.facingDeg(), length * FLAME_LENGTH_SHARE * flicker,
                    length * FLAME_WIDTH_SHARE, ENGINE_COLOR, alpha);
            SpriteDraw.drawAtCenter(spriteCache, escort.spritePath(), screenX, screenY, width, length, null, alpha, angle);
        }
    }
}
