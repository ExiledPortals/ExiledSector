package exiledsector.ui.decoration;

import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.decoration.AbyssSerpent;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.WobblyOrbit;

import java.awt.Color;
import java.util.List;

public final class AbyssSerpentRenderer implements DecorationRenderer {

    record Part(String spritePath, String glowPath, float width, float height, float behindHead, float imageBackOffset) {
    }

    record Serpent(float centerX, float centerY, float orbitRadius, float direction, float secondsPerOrbit, float startAngleRad, WobblyOrbit path) {

        static Serpent of(AbyssSerpent serpent) {
            AbyssSerpent.Orbit orbit = serpent.getOrbit();
            AbyssSerpent.Wobble wobble = serpent.getWobble();
            return new Serpent(serpent.getX(), serpent.getY(), Math.max(MIN_ORBIT_RADIUS, orbit.radius()), orbit.clockwise() ? 1f : -1f,
                    Math.max(MIN_ORBIT_SECONDS, orbit.seconds()), (float) Math.toRadians(orbit.startAngleDegrees()),
                    new WobblyOrbit(wobble.share(), wobble.waves(), (float) Math.toRadians(wobble.phaseDegrees()), 1f, 0f));
        }
    }

    record Pose(float treeX, float treeY, float facingDeg) {
    }

    static final Part[] PARTS = {
            part("system", 160f, 220f, 456f, 36f),
            part("spine", 120f, 100f, 376f, 0f),
            part("Segment", 180f, 160f, 308f, 0f),
            part("spine", 120f, 100f, 236f, 0f),
            part("Segment", 180f, 160f, 168f, 0f),
            part("spine", 120f, 100f, 96f, 0f),
            part("head", 280f, 240f, 0f, 0f)
    };
    static final float SCALE = 1.4f;
    static final float MIN_ORBIT_RADIUS = 200f;
    static final float MIN_ORBIT_SECONDS = 1f;
    private static final float BODY_ALPHA = 0.45f;
    private static final float GLOW_ALPHA = 0.25f;
    private static final Color BODY_TINT = new Color(150, 140, 175);
    private static final Color GLOW_TINT = new Color(170, 60, 255);
    private static final float TANGENT_STEP_RAD = 0.002f;
    private static final float WALK_STEP = 6f;
    private static final float REACH_PER_RADIUS = 1.3f;

    private final SpriteCache spriteCache = new SpriteCache(AbyssSerpentRenderer.class);
    private final WobblyOrbit.Point pathPoint = new WobblyOrbit.Point();
    private final List<Serpent> serpents;
    private float elapsedSeconds;

    AbyssSerpentRenderer(List<AbyssSerpent> serpents) {
        this.serpents = serpents.stream().map(Serpent::of).toList();
    }

    private static Part part(String name, float width, float height, float behindHead, float imageBackOffset) {
        return new Part("graphics/fx/rat_genesis_serpent_" + name + ".png", "graphics/fx/rat_genesis_serpent_" + name + "_glow.png",
                width, height, behindHead, imageBackOffset);
    }

    List<Serpent> serpents() {
        return serpents;
    }

    @Override
    public void advance(float amount) {
        if (!serpents.isEmpty()) {
            elapsedSeconds += amount;
        }
    }

    Pose[] poses(Serpent serpent, float atSeconds) {
        Pose[] poses = new Pose[PARTS.length];
        float orbitRadius = serpent.orbitRadius();
        float angle = serpent.startAngleRad() + serpent.direction() * (float) (Math.PI * 2) * atSeconds / serpent.secondsPerOrbit();
        float angleStep = serpent.direction() * WALK_STEP / orbitRadius;
        serpent.path().locate(serpent.centerX(), serpent.centerY(), orbitRadius, angle, pathPoint);
        float x = pathPoint.x();
        float y = pathPoint.y();
        float walked = 0f;
        float previousX = x;
        float previousY = y;
        float stepLength = 0f;
        for (int index = PARTS.length - 1; index >= 0; index--) {
            float target = PARTS[index].behindHead() * SCALE;
            while (walked < target) {
                angle -= angleStep;
                previousX = x;
                previousY = y;
                serpent.path().locate(serpent.centerX(), serpent.centerY(), orbitRadius, angle, pathPoint);
                x = pathPoint.x();
                y = pathPoint.y();
                stepLength = (float) Math.hypot(x - previousX, y - previousY);
                walked += stepLength;
            }
            float backtrack = stepLength <= 0f ? 0f : (walked - target) / stepLength;
            float partX = x + (previousX - x) * backtrack;
            float partY = y + (previousY - y) * backtrack;
            poses[index] = new Pose(partX, partY, facingDeg(serpent, angle, x, y));
        }
        return poses;
    }

    private float facingDeg(Serpent serpent, float angle, float x, float y) {
        float aheadAngle = angle + serpent.direction() * TANGENT_STEP_RAD;
        serpent.path().locate(serpent.centerX(), serpent.centerY(), serpent.orbitRadius(), aheadAngle, pathPoint);
        return (float) Math.toDegrees(Math.atan2(pathPoint.y() - y, pathPoint.x() - x));
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        float alphaMult = scene.sceneryAlpha() * scene.treeAlpha();
        if (depth != DecorationDepth.CREATURES || alphaMult <= 0f) {
            return;
        }
        TreeViewport viewport = scene.viewport();
        float zoom = viewport.zoom();
        for (Serpent serpent : serpents) {
            float reach = serpent.orbitRadius() * REACH_PER_RADIUS + PARTS[0].behindHead() * SCALE;
            if (!viewport.isVisible(viewport.screenX(serpent.centerX()), viewport.screenY(serpent.centerY()), reach * zoom)) {
                continue;
            }
            Pose[] poses = poses(serpent, elapsedSeconds);
            for (int index = 0; index < PARTS.length; index++) {
                renderPart(viewport, PARTS[index], poses[index], alphaMult);
            }
        }
    }

    private void renderPart(TreeViewport viewport, Part part, Pose pose, float alphaMult) {
        float zoom = viewport.zoom();
        double facingRad = Math.toRadians(pose.facingDeg());
        float imageX = pose.treeX() - (float) Math.cos(facingRad) * part.imageBackOffset() * SCALE;
        float imageY = pose.treeY() - (float) Math.sin(facingRad) * part.imageBackOffset() * SCALE;
        float screenX = viewport.screenX(imageX);
        float screenY = viewport.screenY(imageY);
        float width = part.width() * SCALE * zoom;
        float height = part.height() * SCALE * zoom;
        if (!viewport.isVisible(screenX, screenY, Math.max(width, height))) {
            return;
        }
        float angle = -pose.facingDeg() - 90f;
        SpriteDraw.drawAtCenter(spriteCache, part.spritePath(), screenX, screenY, width, height, BODY_TINT, alphaMult * BODY_ALPHA, angle);
        SpriteDraw.drawAdditiveAtCenter(spriteCache, part.glowPath(), screenX, screenY, width, height, GLOW_TINT, alphaMult * GLOW_ALPHA, angle);
    }
}
