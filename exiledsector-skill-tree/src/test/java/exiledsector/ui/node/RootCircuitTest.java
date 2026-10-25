package exiledsector.ui.node;

import exiledlib.ProjectFiles;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RootCircuitTest {

    private static final float TOLERANCE = 0.002f;
    private static RootCircuit circuit;

    @BeforeAll
    static void loadRealData() throws Exception {
        RealSkillData.load();
        circuit = RootCircuit.parse(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(RootCircuit.DATA_PATH)));
    }

    @AfterAll
    static void clear() {
        RealSkillData.clear();
    }

    private static double turnDegrees(RootCircuit.Trace trace, int corner) {
        double inX = trace.xs()[corner] - trace.xs()[corner - 1];
        double inY = trace.ys()[corner] - trace.ys()[corner - 1];
        double outX = trace.xs()[corner + 1] - trace.xs()[corner];
        double outY = trace.ys()[corner + 1] - trace.ys()[corner];
        double cos = (inX * outX + inY * outY) / (Math.hypot(inX, inY) * Math.hypot(outX, outY));
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }

    @Test
    void everyTraceStartsOnTheRimAndBendsAtMostTwiceAtFortyFiveOrNinetyDegrees() {
        assertFalse(circuit.traces().isEmpty());
        for (RootCircuit.Trace trace : circuit.traces()) {
            assertEquals(1f, (float) Math.hypot(trace.xs()[0], trace.ys()[0]), TOLERANCE);
            int bends = trace.xs().length - 2;
            assertTrue(bends >= 0 && bends <= 2, "bends: " + bends);
            for (int corner = 1; corner <= bends; corner++) {
                double turn = turnDegrees(trace, corner);
                assertTrue(Math.abs(turn - 45) < 0.5 || Math.abs(turn - 90) < 0.5, "turn " + turn);
            }
        }
    }

    @Test
    void theCircuitFitsInsideTheGapAroundEveryRoot() {
        float rootRadius = NodeMetrics.donutRadius(NodeMetrics.NODE_SIZE * SkillTier.ROOT.getSizeMultiplier());
        float circuitReach = circuit.extentRadii() * rootRadius;
        for (RootCircuit.Trace trace : circuit.traces()) {
            int last = trace.xs().length - 1;
            assertTrue(Math.hypot(trace.xs()[last], trace.ys()[last]) < circuit.extentRadii());
        }
        List<SkillNode> roots = new ArrayList<>();
        SkillTree.getAllNodes().values().stream().filter(node -> node.getType().getTier() == SkillTier.ROOT).forEach(roots::add);
        assertFalse(roots.isEmpty());
        for (SkillNode root : roots) {
            double clearance = nearestUnrelatedConnector(root);
            assertTrue(circuitReach < clearance, root.getId() + " reach " + circuitReach + " vs connector at " + clearance);
        }
    }

    private static double nearestUnrelatedConnector(SkillNode root) {
        double nearest = Double.MAX_VALUE;
        for (SkillNode from : SkillTree.getAllNodes().values()) {
            for (String toId : from.getConnectedNodeIds()) {
                SkillNode to = SkillTree.get(toId);
                if (to == null || from == root || to == root) {
                    continue;
                }
                nearest = Math.min(nearest, distanceToSegment(root.getOffsetX(), root.getOffsetY(), from, to));
            }
        }
        return nearest;
    }

    private static double distanceToSegment(double pointX, double pointY, SkillNode from, SkillNode to) {
        double dx = to.getOffsetX() - from.getOffsetX();
        double dy = to.getOffsetY() - from.getOffsetY();
        double lengthSquared = dx * dx + dy * dy;
        double along = lengthSquared == 0 ? 0 : ((pointX - from.getOffsetX()) * dx + (pointY - from.getOffsetY()) * dy) / lengthSquared;
        along = Math.max(0, Math.min(1, along));
        return Math.hypot(from.getOffsetX() + along * dx - pointX, from.getOffsetY() + along * dy - pointY);
    }

    @Test
    void sparkPhasesAreScatteredAcrossTheWholeInterval() {
        List<Float> phases = new ArrayList<>();
        circuit.traces().forEach(trace -> phases.add(trace.phaseSeconds()));
        phases.sort(Float::compare);
        float largestAllowedGap = 2f * RootCircuit.SPARK_INTERVAL_SECONDS / phases.size();
        for (int index = 1; index < phases.size(); index++) {
            assertTrue(phases.get(index) - phases.get(index - 1) <= largestAllowedGap, "gap at " + index);
        }
        assertTrue(phases.get(0) >= 0f && phases.get(phases.size() - 1) < RootCircuit.SPARK_INTERVAL_SECONDS);
    }

    @Test
    void aSparkLeavesThePinAtItsPhaseAndReachesTheSolderDotAfterItsTrip() throws Exception {
        RootCircuit.Trace trace = circuit.traces().get(0);
        float[] distances = new float[4];
        float[] point = new float[2];

        assertEquals(1, trace.sparkDistances(trace.phaseSeconds(), distances));
        assertEquals(0f, distances[0], TOLERANCE);
        trace.pointAt(distances[0], point);
        assertEquals(trace.xs()[0], point[0], TOLERANCE);
        assertEquals(trace.ys()[0], point[1], TOLERANCE);

        assertEquals(1, trace.sparkDistances(trace.phaseSeconds() + trace.tripSeconds() - 0.0005f, distances));
        trace.pointAt(distances[0], point);
        int last = trace.xs().length - 1;
        assertEquals(trace.xs()[last], point[0], TOLERANCE);
        assertEquals(trace.ys()[last], point[1], TOLERANCE);

        assertEquals(0, trace.sparkDistances(trace.phaseSeconds() + trace.tripSeconds() + 0.1f, distances));
    }

    @Test
    void aTripLongerThanTheIntervalHasTwoSparksInFlight() throws Exception {
        RootCircuit longCircuit = RootCircuit.parse(new JSONObject("{\"extentRadii\": 9, \"traces\": [{\"points\": [[1, 0], [8, 0]]}]}"));
        RootCircuit.Trace longTrace = longCircuit.traces().get(0);
        float[] distances = new float[4];

        assertTrue(longTrace.tripSeconds() > RootCircuit.SPARK_INTERVAL_SECONDS);
        assertEquals(2, longTrace.sparkDistances(RootCircuit.SPARK_INTERVAL_SECONDS + 0.5f, distances));
        assertEquals(distances[0] + RootCircuit.SPARK_INTERVAL_SECONDS * RootCircuit.SPARK_SPEED_RADII_PER_SECOND, distances[1], TOLERANCE);
    }
}
