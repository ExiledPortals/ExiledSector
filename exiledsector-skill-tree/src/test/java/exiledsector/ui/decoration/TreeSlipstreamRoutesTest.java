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
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeSlipstreamRoutesTest {

    private static final float ASPECT = 16f / 9f;
    private static final float[] VIEW_HEIGHTS = {4000f, 10000f, 25000f, 60000f};
    private static final float STREAM_REACH_WIDTHS = 0.78f;

    private static final List<Vector2f> BLOCKERS = new ArrayList<>();
    private static final List<Float> BLOCKER_RADII = new ArrayList<>();

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
        for (String volume : SkillTags.VOLUME) {
            RoamVolume roamVolume = TreeRoamVolumes.volume(volume, SkillTree.getDecorations().stars(), SkillTree.getAllNodes().values());
            if (roamVolume != null) {
                block(roamVolume.centerX(), roamVolume.centerY(), roamVolume.outerRadius());
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
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (Vector2f point : route.points()) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        List<Integer> nearby = new ArrayList<>();
        for (int i = 0; i < BLOCKERS.size(); i++) {
            Vector2f blocker = BLOCKERS.get(i);
            float clear = BLOCKER_RADII.get(i) + reach;
            if (blocker.x + clear >= minX && blocker.x - clear <= maxX && blocker.y + clear >= minY && blocker.y - clear <= maxY) {
                nearby.add(i);
            }
        }
        for (float distance = 0f; distance <= route.length(); distance += route.width() / 6f) {
            Vector2f point = route.pointAt(distance);
            for (int i : nearby) {
                Vector2f blocker = BLOCKERS.get(i);
                float dx = point.x - blocker.x;
                float dy = point.y - blocker.y;
                float clear = BLOCKER_RADII.get(i) + reach;
                assertTrue(dx * dx + dy * dy > clear * clear, "the stream touched a blocker at " + blocker);
            }
        }
    }

    private static void block(float x, float y, float radius) {
        BLOCKERS.add(new Vector2f(x, y));
        BLOCKER_RADII.add(radius);
    }
}
