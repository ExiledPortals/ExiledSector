package exiledsector.ui.decoration;

import exiledsector.skills.layout.StaticImage;
import exiledsector.skills.layout.Star;
import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbyssSerpentRendererTest {

    private static final Star ABYSS_STAR = new Star("abyss_star", 1000f, -500f, 400f, "star_red", null, List.of(SkillTags.ABYSS_REGION));

    private static StaticImage cloudAt(float x, float y) {
        return new StaticImage("cloud", x, y, new StaticImage.Shape(900f, 800f, "a.png", null), List.of(SkillTags.ABYSS_REGION));
    }

    private static AbyssSerpentRenderer serpents() {
        return new AbyssSerpentRenderer(ABYSS_STAR, List.of(cloudAt(2500f, -500f), cloudAt(1000f, 1100f)));
    }

    private static float distanceFromStar(AbyssSerpentRenderer.Pose pose) {
        return (float) Math.hypot(pose.treeX() - ABYSS_STAR.getX(), pose.treeY() - ABYSS_STAR.getY());
    }

    private static float signedAngle(AbyssSerpentRenderer.Pose pose) {
        return (float) Math.atan2(pose.treeY() - ABYSS_STAR.getY(), pose.treeX() - ABYSS_STAR.getX());
    }

    @Test
    void theSerpentsCircleAtTheDistanceOfTheAbyssClouds() {
        assertEquals(1550f, serpents().orbitRadius(), 0.01f);
    }

    @Test
    void withoutCloudsTheyCircleAFewStarRadiiOut() {
        AbyssSerpentRenderer serpents = new AbyssSerpentRenderer(ABYSS_STAR, List.of());

        assertEquals(ABYSS_STAR.getRadius() * AbyssSerpentRenderer.DEFAULT_ORBIT_STAR_RADII, serpents.orbitRadius(), 0.01f);
    }

    @Test
    void theTwoSerpentsOrbitInOppositeDirections() {
        AbyssSerpentRenderer serpents = serpents();
        int head = AbyssSerpentRenderer.PARTS.length - 1;
        for (int index = 0; index < AbyssSerpentRenderer.SERPENTS.length; index++) {
            AbyssSerpentRenderer.Serpent serpent = AbyssSerpentRenderer.SERPENTS[index];
            float before = signedAngle(serpents.poses(serpent, 10f)[head]);
            float after = signedAngle(serpents.poses(serpent, 11f)[head]);
            float turned = (float) Math.atan2(Math.sin(after - before), Math.cos(after - before));
            assertEquals(Math.signum(serpent.direction()), Math.signum(turned), "serpent " + index);
        }
        assertEquals(-1f, AbyssSerpentRenderer.SERPENTS[0].direction() * AbyssSerpentRenderer.SERPENTS[1].direction());
    }

    @Test
    void eachSegmentTrailsTheOneAheadAtItsModuleSpacingWithoutGaps() {
        AbyssSerpentRenderer serpents = serpents();
        for (AbyssSerpentRenderer.Serpent serpent : AbyssSerpentRenderer.SERPENTS) {
            for (float seconds = 0f; seconds < 200f; seconds += 13f) {
                AbyssSerpentRenderer.Pose[] poses = serpents.poses(serpent, seconds);
                for (int index = 0; index < poses.length - 1; index++) {
                    float spacing = (AbyssSerpentRenderer.PARTS[index].behindHead() - AbyssSerpentRenderer.PARTS[index + 1].behindHead())
                            * AbyssSerpentRenderer.SCALE;
                    float gap = (float) Math.hypot(poses[index].treeX() - poses[index + 1].treeX(), poses[index].treeY() - poses[index + 1].treeY());
                    assertTrue(gap <= spacing + 0.5f && gap >= spacing * 0.8f, "gap " + gap + " for spacing " + spacing + " at " + seconds);
                }
            }
        }
    }

    @Test
    void theyWindInAndOutButStayWithinTheirBand() {
        AbyssSerpentRenderer serpents = serpents();
        float radius = serpents.orbitRadius();
        for (AbyssSerpentRenderer.Serpent serpent : AbyssSerpentRenderer.SERPENTS) {
            float nearest = Float.MAX_VALUE;
            float farthest = 0f;
            for (float seconds = 0f; seconds < serpent.secondsPerOrbit(); seconds += 0.5f) {
                float distance = distanceFromStar(serpents.poses(serpent, seconds)[AbyssSerpentRenderer.PARTS.length - 1]);
                nearest = Math.min(nearest, distance);
                farthest = Math.max(farthest, distance);
            }
            float band = radius * serpent.windAmplitudeShare();
            assertTrue(farthest - nearest > band, "winds by " + (farthest - nearest));
            assertTrue(nearest >= radius - band - 1f && farthest <= radius + band + 1f);
        }
    }

    @Test
    void theHeadFacesAlongItsPath() {
        AbyssSerpentRenderer serpents = serpents();
        AbyssSerpentRenderer.Serpent serpent = AbyssSerpentRenderer.SERPENTS[0];
        int head = AbyssSerpentRenderer.PARTS.length - 1;
        AbyssSerpentRenderer.Pose now = serpents.poses(serpent, 20f)[head];
        AbyssSerpentRenderer.Pose soon = serpents.poses(serpent, 20.05f)[head];
        float travelDeg = (float) Math.toDegrees(Math.atan2(soon.treeY() - now.treeY(), soon.treeX() - now.treeX()));
        float difference = (travelDeg - now.facingDeg() + 540f) % 360f - 180f;

        assertTrue(Math.abs(difference) < 5f, "facing off by " + difference);
    }
}
