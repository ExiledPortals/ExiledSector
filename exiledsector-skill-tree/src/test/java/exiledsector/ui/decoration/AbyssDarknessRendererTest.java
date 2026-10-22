package exiledsector.ui.decoration;

import exiledsector.ProjectFiles;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.StaticImage;
import exiledsector.skills.loader.SkillTreeLoader;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.TreeViewport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbyssDarknessRendererTest {

    private static final SkillType SMALL = new SkillType.Builder("small", "Small", "a.png", SkillTier.SMALL).build();
    private static final float CANVAS_WIDTH = 1600f;
    private static final float CANVAS_HEIGHT = 900f;

    private record Band(float[] vertexXs, float[] vertexYs, float[] darkness) {
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillNode node(String id, float treeX, float treeY, String... tags) {
        return new SkillNode(id, SMALL, List.of(), treeX, treeY, SkillNodeDecoration.NONE, List.of(tags));
    }

    private static List<Band> bands(AbyssalDepth abyssalDepth, float treeCenterX, float treeCenterY, float zoom) {
        AbyssDarknessRenderer renderer = new AbyssDarknessRenderer(abyssalDepth);
        TreeViewport viewport = new TreeViewport(CANVAS_WIDTH / 2f - treeCenterX * zoom, CANVAS_HEIGHT / 2f + treeCenterY * zoom, zoom);
        assertTrue(renderer.aimAt(viewport));
        List<Band> bands = new ArrayList<>();
        renderer.forEachBand(0f, 0f, CANVAS_WIDTH, CANVAS_HEIGHT, (vertexXs, vertexYs, vertexDarkness, vertexCount) ->
                bands.add(new Band(Arrays.copyOf(vertexXs, vertexCount), Arrays.copyOf(vertexYs, vertexCount),
                        Arrays.copyOf(vertexDarkness, vertexCount))));
        return bands;
    }

    private static float coveredArea(List<Band> bands) {
        float area = 0f;
        for (Band band : bands) {
            for (int vertex = 0; vertex < band.vertexXs().length; vertex++) {
                int next = (vertex + 1) % band.vertexXs().length;
                area += band.vertexXs()[vertex] * band.vertexYs()[next] - band.vertexXs()[next] * band.vertexYs()[vertex];
            }
        }
        return Math.abs(area) / 2f;
    }

    @Test
    void theDarknessPointsFromTheTreeCentreTowardsTheAbyssAndStartsBeyondTheMainTree() {
        AbyssalDepth abyssalDepth = AbyssalDepth.of(List.of(node("main", -1000f, 0f, SkillTags.CORE_REGION),
                node("abyss_a", -10000f, 0f, SkillTags.ABYSS_REGION), node("abyss_b", -12000f, 0f, SkillTags.ABYSS_REGION)));

        assertNotNull(abyssalDepth);
        assertEquals(-1f, abyssalDepth.directionX(), 1e-6f);
        assertEquals(0f, abyssalDepth.directionY(), 1e-6f);
        assertTrue(abyssalDepth.depthAt(-1000f, 0f) < 0f);
        assertEquals(1f, AbyssDarknessRenderer.darkness(abyssalDepth.depthAt(-10000f, 0f)));
    }

    @Test
    void theThreatVolumeSitsInAbyssalHyperspaceEvenWithoutTheAbyss() {
        AbyssalDepth abyssalDepth = AbyssalDepth.of(List.of(node("main", -1000f, 0f, SkillTags.CORE_REGION),
                node("fabricator", -10000f, 0f, SkillTags.THREAT_REGION)));

        assertNotNull(abyssalDepth);
        assertEquals(1f, AbyssDarknessRenderer.darkness(abyssalDepth.depthAt(-10000f, 0f)));
    }

    @Test
    void aTreeWithoutAnAbyssHasNoDarkness() {
        assertNull(AbyssalDepth.of(List.of(node("main", 500f, 500f, SkillTags.CORE_REGION))));
    }

    @Test
    void anAbyssOverlappingTheMainTreeHasNoDarkness() {
        assertNull(AbyssalDepth.of(List.of(node("main", -5000f, 0f), node("abyss", -4000f, 0f, SkillTags.ABYSS_REGION))));
    }

    @Test
    void theDarknessEasesInAndOut() {
        assertEquals(0f, AbyssDarknessRenderer.darkness(-3f));
        assertEquals(0.5f, AbyssDarknessRenderer.darkness(0.5f), 1e-6f);
        assertEquals(1f, AbyssDarknessRenderer.darkness(4f));
        assertTrue(AbyssDarknessRenderer.darkness(0.1f) < 0.1f);
    }

    @Test
    void theBandsTileTheCanvasExactlyAcrossTheFade() {
        AbyssalDepth abyssalDepth = new AbyssalDepth(-1f, 0f, 1000f, 2000f);

        List<Band> bands = bands(abyssalDepth, -1500f, 0f, 1f);

        assertEquals(CANVAS_WIDTH * CANVAS_HEIGHT * 1300f / 1600f, coveredArea(bands), 1f);
        assertEquals(AbyssDarknessRenderer.FADE_BANDS + 1, bands.size());
        for (Band band : bands) {
            for (float vertexDarkness : band.darkness()) {
                assertTrue(vertexDarkness >= 0f && vertexDarkness <= 1f);
            }
        }
    }

    @Test
    void deepInTheAbyssOneSolidPolygonCoversTheCanvas() {
        AbyssalDepth abyssalDepth = new AbyssalDepth(-1f, 0f, 1000f, 2000f);

        List<Band> bands = bands(abyssalDepth, -9000f, 0f, 1f);

        assertEquals(1, bands.size());
        assertEquals(CANVAS_WIDTH * CANVAS_HEIGHT, coveredArea(bands), 1f);
        for (float vertexDarkness : bands.get(0).darkness()) {
            assertEquals(1f, vertexDarkness);
        }
    }

    @Test
    void theMainTreeDrawsNothing() {
        AbyssalDepth abyssalDepth = new AbyssalDepth(-1f, 0f, 1000f, 2000f);

        assertTrue(bands(abyssalDepth, 0f, 0f, 1f).isEmpty());
    }

    @Test
    void theRealTreeKeepsEveryMainNodeLitAndSinksEveryAbyssNodeAndCloudIntoFullDarkness() throws Exception {
        RealSkillData.load();
        AbyssalDepth abyssalDepth = AbyssalDepth.of(SkillTree.getAllNodes().values());

        assertNotNull(abyssalDepth);
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            float darkness = AbyssDarknessRenderer.darkness(abyssalDepth.depthAt(node.getX(), node.getY()));
            if (AbyssalDepth.isAbyssal(node)) {
                assertEquals(1f, darkness, node::getId);
            } else {
                assertEquals(0f, darkness, node::getId);
            }
        }
        List<StaticImage> images = SkillTreeLoader.parseStaticImages(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(RealSkillData.TREE_FILE)));
        images.stream().filter(image -> image.getTags().contains(SkillTags.ABYSS_REGION)).forEach(image -> {
            float cloudReach = (float) Math.hypot(image.getWidth(), image.getHeight()) / 2f;
            float nearestX = image.getX() - abyssalDepth.directionX() * cloudReach;
            float nearestY = image.getY() - abyssalDepth.directionY() * cloudReach;
            assertEquals(1f, AbyssDarknessRenderer.darkness(abyssalDepth.depthAt(nearestX, nearestY)), image::getId);
        });
    }
}
