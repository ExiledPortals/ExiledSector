package exiledsector.ui.exoship;

import exiledsector.ui.TreeViewport;
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
            new Escort("graphics/fx/rat_hypatia.png", 108f, 160f, 0.07f, 0.85f, 0.5f, 24f, 1f, 0f, 20f, 0.06f, 3),
            new Escort("graphics/fx/rat_dawnblade.png", 60f, 60f, 0.035f, 0.62f, 0.55f, 13f, -1f, 2f, -35f, 0.1f, 4),
            new Escort("graphics/fx/rat_nightblade.png", 60f, 60f, 0.035f, 0.72f, 0.45f, 15f, 1f, 4f, 60f, 0.08f, 5),
            new Escort("graphics/fx/rat_protostar.png", 60f, 60f, 0.03f, 0.56f, 0.6f, 11f, -1f, 1f, 110f, 0.12f, 3)
    };
    static final float BEHIND_ALPHA = 0.7f;
    private static final String ENGINE_GLOW_PATH = "graphics/fx/engineglow32.png";
    private static final Color ENGINE_COLOR = new Color(247, 121, 86);
    private static final float TANGENT_STEP_RAD = 0.002f;

    private final SpriteCache spriteCache = new SpriteCache(ExoshipEscorts.class);
    private float elapsedSeconds;

    void advance(float amount) {
        elapsedSeconds += amount;
    }

    float elapsedSeconds() {
        return elapsedSeconds;
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
        for (Escort escort : ESCORTS) {
            Pose pose = pose(escort, centerX, centerY, shipLength, elapsedSeconds);
            if (pose.inFront() != inFront) {
                continue;
            }
            float length = escort.lengthShare() * shipLength * zoom;
            float width = length * escort.width() / escort.height();
            float screenX = viewport.screenX(pose.treeX());
            float screenY = viewport.screenY(pose.treeY());
            if (!viewport.isVisible(screenX, screenY, length)) {
                continue;
            }
            float alpha = alphaMult * (inFront ? 1f : BEHIND_ALPHA);
            float angle = -pose.facingDeg() - 90f;
            double screenFacing = Math.toRadians(-pose.facingDeg());
            float engineX = screenX - (float) Math.cos(screenFacing) * length * 0.55f;
            float engineY = screenY - (float) Math.sin(screenFacing) * length * 0.55f;
            SpriteDraw.drawAdditiveAtCenter(spriteCache, ENGINE_GLOW_PATH, engineX, engineY, width * 0.6f, length * 0.6f, ENGINE_COLOR, alpha * 0.8f,
                    angle);
            SpriteDraw.drawAtCenter(spriteCache, escort.spritePath(), screenX, screenY, width, length, null, alpha, angle);
        }
    }
}
