package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeBeamsTest {

    @Test
    void litLooksWhitenTheCoreAndSplitIntoLayers() {
        NodeBeams beams = new NodeBeams(new NodeRingArt());
        BeamStroke.Look look = beams.lit(new Color(100, 0, 255));

        assertEquals(new Color(209, 179, 255), look.core());
        assertSame(look, beams.lit(new Color(100, 0, 255)));
        assertEquals(0f, look.fringeOnly().coreAlpha());
        assertEquals(look.fringeAlpha(), look.fringeOnly().fringeAlpha());
        assertEquals(0f, look.coreOnly().fringeAlpha());
        assertEquals(look.coreAlpha(), look.coreOnly().coreAlpha());
    }

    @Test
    void beamsKeepAMinimumWidthWhenZoomedFarOut() {
        assertEquals(NodeBeams.widthScale(NodeBeams.MIN_NODE_ZOOM), NodeBeams.widthScale(0.1f));
        assertEquals(1f, NodeBeams.widthScale(2f));
    }

    @Test
    void ringsHugTheIconInsideTheirOwnWidth() {
        float ringRadius = NodeBeams.huggingRingRadius(30f, 1f);

        assertTrue(ringRadius > 30f);
        assertTrue(ringRadius - NodeBeams.ringEdge(1f) < 30f);
        assertTrue(NodeBeams.ringOuterEdge(ringRadius, 1f) < ringRadius + NodeBeams.ringEdge(1f));
    }

    @Test
    void lanesStopWhereTheirVisibleEdgesMeetTheRing() {
        float laneHalf = NodeBeams.laneEdge(1f) * NodeBeams.LANE_VISIBLE_SHARE;
        float reach = NodeBeams.laneReach(40f, 1f);

        assertEquals(40f, (float) Math.hypot(reach, laneHalf), 0.001f);
        assertEquals(0f, NodeBeams.laneReach(1f, 1f));
    }

    @Test
    void lightsSetRingStrength() {
        NodeBeams beams = new NodeBeams(new NodeRingArt());

        assertEquals(1f, beams.lightAlpha(NodeBeams.RingLight.LIT));
        assertEquals(NodeBeams.DIM_ALPHA, beams.lightAlpha(NodeBeams.RingLight.DIM));
        assertEquals(NodeBeams.DULL_ALPHA, beams.lightAlpha(NodeBeams.RingLight.DULL));
    }
}
