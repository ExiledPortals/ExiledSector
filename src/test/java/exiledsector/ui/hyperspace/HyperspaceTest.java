package exiledsector.ui.hyperspace;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.Rotation;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.Star;
import exiledsector.skills.layout.StaticImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperspaceTest {

    private static Star star(String id, float x, float y, String region) {
        return new Star(id, x, y, 400f, "star_yellow", null, region == null ? List.of() : List.of(region));
    }

    private static StaticImage image(String id, String region) {
        return new StaticImage(id, 3000f, 7000f, 3400f, 2000f, "a.png", new Rotation(0f, 0f), List.of(region));
    }

    @Test
    void everyRegionStarIsAnAnchorAndUntaggedStarsAreNot() {
        List<HyperspaceAnchor> anchors = HyperspaceAnchor.collect(
                List.of(star("sun", 0f, 0f, "core"), star("stray", 5f, 5f, null), star("dictat", -6000f, -4000f, "sindrian_dictat")),
                List.of());

        assertEquals(List.of("sun", "dictat"), anchors.stream().map(HyperspaceAnchor::id).toList());
        assertEquals("sindrian_dictat", anchors.get(1).region());
    }

    @Test
    void aRegionWithoutAStarIsAnchoredOnItsFirstImage() {
        List<HyperspaceAnchor> anchors = HyperspaceAnchor.collect(
                List.of(star("sun", 0f, 0f, "core")),
                List.of(image("cloud", "core"), image("luddic_nebula", "luddic"), image("luddic_wisp", "luddic")));

        assertEquals(List.of("sun", "luddic_nebula"), anchors.stream().map(HyperspaceAnchor::id).toList());
        assertEquals(500f, anchors.get(1).radius());
    }

    @Test
    void theMapCameraFitsEveryAnchorAndLeavesRoomForLabelsAbove() {
        List<HyperspaceAnchor> anchors = List.of(
                new HyperspaceAnchor("a", "core", -1000f, 0f, 100f, true),
                new HyperspaceAnchor("b", "pirate", 1000f, 0f, 100f, true));

        HyperspaceCamera camera = HyperspaceCamera.fit(anchors, 150f, 1000f, 1000f, 50f, 40f);

        float spanX = 2000f + 2f * 150f * HyperspaceCamera.ANCHOR_REACH;
        assertEquals(900f / spanX, camera.zoom(), 1e-5f);
        assertEquals(0f, camera.x(), 1e-3f);
        assertEquals(-20f / camera.zoom(), camera.y(), 1e-2f);
    }

    @Test
    void theTransitionGlidesFromTheTreeToTheMapAndBack() {
        HyperspaceTransition transition = new HyperspaceTransition();
        HyperspaceCamera tree = new HyperspaceCamera(100f, 200f, 0.2f);
        HyperspaceCamera map = new HyperspaceCamera(0f, 0f, 0.05f);
        assertFalse(transition.isActive());

        transition.enter(tree, map);
        assertTrue(transition.isActive());
        assertEquals(tree, transition.camera());

        transition.advance(HyperspaceTransition.DURATION_SECONDS / 2f);
        HyperspaceCamera halfway = transition.camera();
        assertEquals(0.1f, halfway.zoom(), 1e-5f);
        assertEquals(50f, halfway.x(), 1e-3f);
        assertFalse(transition.isOnMap());

        transition.advance(HyperspaceTransition.DURATION_SECONDS);
        assertTrue(transition.isOnMap());
        assertEquals(0.05f, transition.camera().zoom(), 1e-6f);
        assertEquals(0f, transition.treeAlpha());
        assertEquals(1f, transition.labelAlpha());

        transition.leaveTo(transition.camera(), tree);
        transition.advance(HyperspaceTransition.DURATION_SECONDS * 2f);
        assertFalse(transition.isActive());
        assertEquals(tree, transition.camera());
        assertEquals(1f, transition.treeAlpha());
        assertEquals(0f, transition.labelAlpha());
    }

    @Test
    void leavingStartsFromWhereverTheMapWasPannedTo() {
        HyperspaceTransition transition = new HyperspaceTransition();
        transition.enter(new HyperspaceCamera(0f, 0f, 0.2f), new HyperspaceCamera(0f, 0f, 0.05f));
        transition.advance(HyperspaceTransition.DURATION_SECONDS);

        HyperspaceCamera panned = new HyperspaceCamera(3000f, -2000f, 0.05f);
        transition.leaveTo(panned, new HyperspaceCamera(0f, 0f, 0.2f));

        assertEquals(panned, transition.camera());
    }

    @Test
    void zoomingInFromTheMapKeepsThePointUnderTheCursorStillThroughout() {
        HyperspaceTransition transition = new HyperspaceTransition();
        HyperspaceCamera map = new HyperspaceCamera(0f, 0f, 0.05f);
        transition.enter(new HyperspaceCamera(0f, 0f, 0.2f), map);
        transition.advance(HyperspaceTransition.DURATION_SECONDS);
        float pivotX = 4000f;
        float pivotY = -2000f;
        float screenOffsetX = (pivotX - map.x()) * map.zoom();
        float screenOffsetY = (pivotY - map.y()) * map.zoom();

        transition.zoomInAbout(map, pivotX, pivotY, 0.2f);
        for (int step = 0; step < 4; step++) {
            transition.advance(HyperspaceTransition.DURATION_SECONDS / 4f);
            HyperspaceCamera camera = transition.camera();
            assertEquals(screenOffsetX, (pivotX - camera.x()) * camera.zoom(), 1e-2f);
            assertEquals(screenOffsetY, (pivotY - camera.y()) * camera.zoom(), 1e-2f);
        }
        assertFalse(transition.isActive());
        assertEquals(0.2f, transition.camera().zoom(), 1e-6f);
    }

    @Test
    void everyStarGrowsToTheSameSizeOnTheMapWhileNebulaeStayPut() {
        HyperspaceTransition transition = new HyperspaceTransition();
        transition.enter(new HyperspaceCamera(0f, 0f, 0.2f), new HyperspaceCamera(0f, 0f, 0.05f));
        assertEquals(0f, transition.mapAmount());

        transition.advance(HyperspaceTransition.DURATION_SECONDS);

        assertEquals(1f, transition.mapAmount());
        HyperspaceAnchor core = new HyperspaceAnchor("sun", "core", 0f, 0f, 150f, true);
        HyperspaceAnchor pirate = new HyperspaceAnchor("pirate", "pirate", 8000f, 0f, 400f, true);
        HyperspaceAnchor nebula = new HyperspaceAnchor("nebula", "luddic", 3000f, 7000f, 850f, false);
        float mapRadius = HyperspaceAnchor.mapStarRadius(List.of(core, pirate, nebula), 1.5f);
        assertEquals(600f, mapRadius);

        assertEquals(150f, core.displayRadius(mapRadius, 0f));
        assertEquals(375f, core.displayRadius(mapRadius, 0.5f));
        assertEquals(600f, core.displayRadius(mapRadius, 1f));
        assertEquals(600f, pirate.displayRadius(mapRadius, 1f));
        assertEquals(850f, nebula.displayRadius(mapRadius, 1f));
    }

    @Test
    void leavingTowardAnAnchorEndsCentredOnIt() {
        HyperspaceTransition transition = new HyperspaceTransition();
        transition.enter(new HyperspaceCamera(0f, 0f, 0.2f), new HyperspaceCamera(0f, 0f, 0.05f));
        transition.advance(HyperspaceTransition.DURATION_SECONDS);

        HyperspaceCamera destination = new HyperspaceCamera(-6000f, -4000f, 0.35f);
        transition.leaveTo(transition.camera(), destination);
        transition.advance(HyperspaceTransition.DURATION_SECONDS);

        assertFalse(transition.isActive());
        assertEquals(destination, transition.camera());
    }

    @Test
    void theTreeHasFadedBeforeTheLabelsAppear() {
        HyperspaceTransition transition = new HyperspaceTransition();
        transition.enter(new HyperspaceCamera(0f, 0f, 0.2f), new HyperspaceCamera(0f, 0f, 0.05f));

        transition.advance(HyperspaceTransition.DURATION_SECONDS * HyperspaceTransition.TREE_FADE_END);

        assertEquals(0f, transition.treeAlpha(), 1e-6f);
        assertEquals(0f, transition.labelAlpha(), 1e-6f);
    }

    private static SkillNode wormhole(String id, String region) {
        SkillType type = new SkillType.Builder("wormhole", "Wormhole", "a.png", SkillTier.WORMHOLE).build();
        return new SkillNode(id, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }

    @Test
    void wormholePairsBecomeRoutesBetweenTheStarsOfTheirEndsRegions() {
        HyperspaceAnchor core = new HyperspaceAnchor("sun", "core", 0f, 0f, 150f, true);
        HyperspaceAnchor league = new HyperspaceAnchor("league", "persean_league", -7800f, 1600f, 400f, true);
        HyperspaceAnchor kesteven = new HyperspaceAnchor("kesteven", "kesteven", -11200f, 6600f, 400f, true);
        List<HyperspaceAnchor> anchors = List.of(core, league, kesteven);

        List<HyperspaceRoute> routes = HyperspaceRoute.between(anchors, List.of(
                new SkillTreeTopology.WormholePair(wormhole("league_1", "core"), wormhole("league_2", "persean_league")),
                new SkillTreeTopology.WormholePair(wormhole("league_2b", "persean_league"), wormhole("league_1b", "core")),
                new SkillTreeTopology.WormholePair(wormhole("kesteven_1", "kesteven"), wormhole("kesteven_2", "persean_league")),
                new SkillTreeTopology.WormholePair(wormhole("inner_a", "core"), wormhole("inner_b", "core")),
                new SkillTreeTopology.WormholePair(wormhole("lost_a", "core"), wormhole("lost_b", "luddic"))));

        assertEquals(List.of(new HyperspaceRoute(core, league), new HyperspaceRoute(kesteven, league)), routes);
    }
}
