package exiledsector.ui.decoration;

import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.StaticImage;
import exiledsector.skills.layout.Star;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

public final class AbyssSerpentRenderer {

    record Part(String spritePath, String glowPath, float width, float height, float behindHead, float imageBackOffset) {
    }

    record Serpent(float direction, float secondsPerOrbit, float startAngleRad, float windAmplitudeShare, int windWaves, float windPhaseRad) {
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
    static final Serpent[] SERPENTS = {
            new Serpent(1f, 150f, 0f, 0.12f, 9, 0f),
            new Serpent(-1f, 190f, (float) Math.PI, 0.15f, 7, 1.3f)
    };
    static final float SCALE = 1.4f;
    static final float DEFAULT_ORBIT_STAR_RADII = 4f;
    private static final float BODY_ALPHA = 0.45f;
    private static final float GLOW_ALPHA = 0.25f;
    private static final Color BODY_TINT = new Color(150, 140, 175);
    private static final Color GLOW_TINT = new Color(170, 60, 255);
    private static final float TANGENT_STEP_RAD = 0.002f;
    private static final float WALK_STEP = 6f;

    private final SpriteCache spriteCache = new SpriteCache(AbyssSerpentRenderer.class);
    private final float centerX;
    private final float centerY;
    private final float orbitRadius;
    private final boolean present;
    private float elapsedSeconds;

    AbyssSerpentRenderer(Star abyssStar, List<StaticImage> clouds) {
        present = abyssStar != null;
        centerX = present ? abyssStar.getX() : 0f;
        centerY = present ? abyssStar.getY() : 0f;
        orbitRadius = present ? orbitRadius(abyssStar, clouds) : 0f;
    }

    public static AbyssSerpentRenderer forActiveTree() {
        Star abyssStar = SkillTree.getStars().stream().filter(star -> SkillTags.ABYSS_REGION.equals(star.getRegion())).findFirst().orElse(null);
        List<StaticImage> clouds = SkillTree.getStaticImages().stream().filter(image -> SkillTags.ABYSS_REGION.equals(image.getRegion())).toList();
        return new AbyssSerpentRenderer(abyssStar, clouds);
    }

    private static Part part(String name, float width, float height, float behindHead, float imageBackOffset) {
        return new Part("graphics/fx/rat_genesis_serpent_" + name + ".png", "graphics/fx/rat_genesis_serpent_" + name + "_glow.png",
                width, height, behindHead, imageBackOffset);
    }

    static float orbitRadius(Star abyssStar, List<StaticImage> clouds) {
        if (clouds.isEmpty()) {
            return abyssStar.getRadius() * DEFAULT_ORBIT_STAR_RADII;
        }
        float totalDistance = 0f;
        for (StaticImage cloud : clouds) {
            totalDistance += (float) Math.hypot(cloud.getX() - abyssStar.getX(), cloud.getY() - abyssStar.getY());
        }
        return totalDistance / clouds.size();
    }

    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    float orbitRadius() {
        return orbitRadius;
    }

    Pose[] poses(Serpent serpent, float atSeconds) {
        Pose[] poses = new Pose[PARTS.length];
        float angle = serpent.startAngleRad() + serpent.direction() * (float) (Math.PI * 2) * atSeconds / serpent.secondsPerOrbit();
        float angleStep = serpent.direction() * WALK_STEP / orbitRadius;
        float x = pathX(serpent, angle);
        float y = pathY(serpent, angle);
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
                x = pathX(serpent, angle);
                y = pathY(serpent, angle);
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
        return (float) Math.toDegrees(Math.atan2(pathY(serpent, aheadAngle) - y, pathX(serpent, aheadAngle) - x));
    }

    private float pathRadius(Serpent serpent, float angle) {
        return orbitRadius * (1f + serpent.windAmplitudeShare() * (float) Math.sin(serpent.windWaves() * angle + serpent.windPhaseRad()));
    }

    private float pathX(Serpent serpent, float angle) {
        return centerX + pathRadius(serpent, angle) * (float) Math.cos(angle);
    }

    private float pathY(Serpent serpent, float angle) {
        return centerY + pathRadius(serpent, angle) * (float) Math.sin(angle);
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if (!present || alphaMult <= 0f) {
            return;
        }
        float zoom = viewport.zoom();
        float reach = orbitRadius * 1.3f + PARTS[0].behindHead() * SCALE;
        if (!viewport.isVisible(viewport.screenX(centerX), viewport.screenY(centerY), reach * zoom)) {
            return;
        }
        for (Serpent serpent : SERPENTS) {
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
