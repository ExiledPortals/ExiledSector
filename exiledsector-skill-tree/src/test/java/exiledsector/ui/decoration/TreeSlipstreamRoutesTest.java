package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.slipstream.SlipstreamRoute;
import exiledsector.ui.slipstream.SlipstreamRoutes;
import exiledsector.ui.slipstream.ViewWindow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TreeSlipstreamRoutesTest {

    private static final float ASPECT = 16f / 9f;
    private static final float[] VIEW_HEIGHTS = {4000f, 10000f, 25000f, 60000f};
    private static final float STREAM_REACH_WIDTHS = 0.78f;
    private static final float BUCKET_SIZE = 2000f;

    private static final List<float[]> VOLUMES = new ArrayList<>();
    private static final Map<Long, List<float[]>> SMALL_BLOCKERS = new HashMap<>();
    private static float largestSmallRadius;

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
        for (String volume : SkillTags.VOLUME) {
            RoamVolume roamVolume = TreeRoamVolumes.volume(volume, SkillTree.getDecorations().stars(), SkillTree.getAllNodes().values());
            if (roamVolume != null) {
                VOLUMES.add(new float[] {roamVolume.centerX(), roamVolume.centerY(), roamVolume.outerRadius()});
            }
        }
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            block(node.getX(), node.getY(), TreeSlipstreamRoutes.NODE_RADIUS);
        }
        for (TreeTopology.Connector<SkillNode> connector : SkillTree.topology().connectors()) {
            SkillNode from = connector.from();
            SkillNode to = connector.to();
            ConnectorCurve curve = connector.curve();
            float controlX = curve == null ? (from.getX() + to.getX()) / 2f : 2f * curve.getControlOffsetX() - (from.getX() + to.getX()) / 2f;
            float controlY = curve == null ? (from.getY() + to.getY()) / 2f : 2f * curve.getControlOffsetY() - (from.getY() + to.getY()) / 2f;
            for (int step = 0; step <= 40; step++) {
                float t = step / 40f;
                float u = 1f - t;
                block(u * u * from.getX() + 2f * u * t * controlX + t * t * to.getX(), u * u * from.getY() + 2f * u * t * controlY + t * t * to.getY(),
                        TreeSlipstreamRoutes.CONNECTOR_RADIUS);
            }
        }
    }

    @AfterAll
    static void clear() {
        RealSkillData.clear();
        VOLUMES.clear();
        SMALL_BLOCKERS.clear();
    }

    @Test
    void routesOnTheRealTreeNeverTouchAVolumeNodeOrConnector() {
        SlipstreamRoutes routes = TreeSlipstreamRoutes.forActiveTree();
        Random random = new Random(11);
        int planned = 0;
        int attempts = 0;
        for (float viewHeight : VIEW_HEIGHTS) {
            for (int i = 0; i < 12; i++) {
                float centerX = -55000f + random.nextFloat() * 95000f;
                float centerY = -38000f + random.nextFloat() * 74000f;
                ViewWindow view = new ViewWindow(centerX - viewHeight * ASPECT / 2f, centerY - viewHeight / 2f, centerX + viewHeight * ASPECT / 2f,
                        centerY + viewHeight / 2f);
                attempts++;
                SlipstreamRoute route = routes.plan(view, random);
                if (route == null) {
                    continue;
                }
                planned++;
                assertClear(route);
            }
        }
        assertTrue(planned * 2 >= attempts, "planned " + planned + " of " + attempts);
    }

    private static void assertClear(SlipstreamRoute route) {
        float reach = route.width() * STREAM_REACH_WIDTHS;
        int bucketReach = (int) Math.ceil((largestSmallRadius + reach) / BUCKET_SIZE);
        for (float distance = 0f; distance <= route.length(); distance += route.width() / 6f) {
            Vector2f point = route.pointAt(distance);
            for (float[] volume : VOLUMES) {
                assertApart(point, volume, reach);
            }
            int bucketX = bucket(point.x);
            int bucketY = bucket(point.y);
            for (int x = bucketX - bucketReach; x <= bucketX + bucketReach; x++) {
                for (int y = bucketY - bucketReach; y <= bucketY + bucketReach; y++) {
                    for (float[] blocker : SMALL_BLOCKERS.getOrDefault(key(x, y), List.of())) {
                        assertApart(point, blocker, reach);
                    }
                }
            }
        }
    }

    private static void assertApart(Vector2f point, float[] blocker, float reach) {
        float dx = point.x - blocker[0];
        float dy = point.y - blocker[1];
        float clear = blocker[2] + reach;
        if (dx * dx + dy * dy <= clear * clear) {
            fail("the stream touched a blocker at (" + blocker[0] + ", " + blocker[1] + ")");
        }
    }

    private static void block(float x, float y, float radius) {
        largestSmallRadius = Math.max(largestSmallRadius, radius);
        SMALL_BLOCKERS.computeIfAbsent(key(bucket(x), bucket(y)), unused -> new ArrayList<>()).add(new float[] {x, y, radius});
    }

    private static int bucket(float coordinate) {
        return (int) Math.floor(coordinate / BUCKET_SIZE);
    }

    private static long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xffffffffL);
    }
}
