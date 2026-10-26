package exiledsector.ui.decoration;

import exiledsector.skills.decoration.AbyssSerpent;
import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbyssSerpentRendererTest {

    private static final float CENTER_X = 1000f;
    private static final float CENTER_Y = -500f;
    private static final float ORBIT_RADIUS = 1550f;

    private static AbyssSerpent serpent(String id, boolean clockwise, float orbitRadius, float seconds, AbyssSerpent.Wobble wobble) {
        return new AbyssSerpent(id, CENTER_X, CENTER_Y, new AbyssSerpent.Orbit(orbitRadius, seconds, clockwise, 0f), wobble, List.of(SkillTags.ABYSS_REGION));
    }

    private static AbyssSerpentRenderer serpents() {
        return new AbyssSerpentRenderer(List.of(
                serpent("clockwise", true, ORBIT_RADIUS, 150f, new AbyssSerpent.Wobble(0.12f, 9, 0f)),
                serpent("anticlockwise", false, ORBIT_RADIUS, 190f, new AbyssSerpent.Wobble(0.15f, 7, 74.5f))));
    }

    private static float distanceFromCentre(AbyssSerpentRenderer.Pose pose) {
        return (float) Math.hypot(pose.treeX() - CENTER_X, pose.treeY() - CENTER_Y);
    }

    private static float signedAngle(AbyssSerpentRenderer.Pose pose) {
        return (float) Math.atan2(pose.treeY() - CENTER_Y, pose.treeX() - CENTER_X);
    }

    @Test
    void eachSerpentDecorationBecomesOneSerpentOnItsOwnOrbit() {
        List<AbyssSerpentRenderer.Serpent> serpents = serpents().serpents();

        assertEquals(2, serpents.size());
        assertEquals(ORBIT_RADIUS, serpents.get(0).orbitRadius(), 0.01f);
        assertEquals(CENTER_X, serpents.get(1).centerX(), 0.01f);
    }

    @Test
    void clockwiseSerpentsTurnOneWayAndTheOthersTheOther() {
        AbyssSerpentRenderer renderer = serpents();
        int head = AbyssSerpentRenderer.PARTS.length - 1;
        for (AbyssSerpentRenderer.Serpent serpent : renderer.serpents()) {
            float before = signedAngle(renderer.poses(serpent, 10f)[head]);
            float after = signedAngle(renderer.poses(serpent, 11f)[head]);
            float turned = (float) Math.atan2(Math.sin(after - before), Math.cos(after - before));
            assertEquals(Math.signum(serpent.direction()), Math.signum(turned));
        }
        assertEquals(1f, renderer.serpents().get(0).direction());
        assertEquals(-1f, renderer.serpents().get(1).direction());
    }

    @Test
    void eachSegmentTrailsTheOneAheadAtItsModuleSpacingWithoutGaps() {
        AbyssSerpentRenderer renderer = serpents();
        for (AbyssSerpentRenderer.Serpent serpent : renderer.serpents()) {
            for (float seconds = 0f; seconds < 200f; seconds += 13f) {
                AbyssSerpentRenderer.Pose[] poses = renderer.poses(serpent, seconds);
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
        AbyssSerpentRenderer renderer = serpents();
        for (AbyssSerpentRenderer.Serpent serpent : renderer.serpents()) {
            float nearest = Float.MAX_VALUE;
            float farthest = 0f;
            for (float seconds = 0f; seconds < serpent.secondsPerOrbit(); seconds += 0.5f) {
                float distance = distanceFromCentre(renderer.poses(serpent, seconds)[AbyssSerpentRenderer.PARTS.length - 1]);
                nearest = Math.min(nearest, distance);
                farthest = Math.max(farthest, distance);
            }
            float band = serpent.orbitRadius() * serpent.path().wobbleShare();
            assertTrue(farthest - nearest > band, "winds by " + (farthest - nearest));
            assertTrue(nearest >= serpent.orbitRadius() - band - 1f && farthest <= serpent.orbitRadius() + band + 1f);
        }
    }

    @Test
    void theHeadFacesAlongItsPath() {
        AbyssSerpentRenderer renderer = serpents();
        AbyssSerpentRenderer.Serpent serpent = renderer.serpents().get(0);
        int head = AbyssSerpentRenderer.PARTS.length - 1;
        AbyssSerpentRenderer.Pose now = renderer.poses(serpent, 20f)[head];
        AbyssSerpentRenderer.Pose soon = renderer.poses(serpent, 20.05f)[head];
        float travelDeg = (float) Math.toDegrees(Math.atan2(soon.treeY() - now.treeY(), soon.treeX() - now.treeX()));
        float difference = (travelDeg - now.facingDeg() + 540f) % 360f - 180f;

        assertTrue(Math.abs(difference) < 5f, "facing off by " + difference);
    }

    @Test
    void aTinyOrbitOrZeroOrbitTimeStillGivesAWalkableOrbit() {
        AbyssSerpentRenderer renderer = new AbyssSerpentRenderer(List.of(serpent("tiny", true, 0f, 0f, AbyssSerpent.Wobble.NONE)));
        AbyssSerpentRenderer.Serpent serpent = renderer.serpents().get(0);

        assertEquals(AbyssSerpentRenderer.MIN_ORBIT_RADIUS, serpent.orbitRadius(), 0.01f);
        assertEquals(AbyssSerpentRenderer.MIN_ORBIT_SECONDS, serpent.secondsPerOrbit(), 0.01f);
        for (AbyssSerpentRenderer.Pose pose : renderer.poses(serpent, 3f)) {
            assertTrue(Float.isFinite(pose.treeX()) && Float.isFinite(pose.treeY()) && Float.isFinite(pose.facingDeg()));
        }
    }
}
