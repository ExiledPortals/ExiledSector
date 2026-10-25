package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    void lightsSetRingStrength() {
        NodeBeams beams = new NodeBeams(new NodeRingArt());

        assertEquals(1f, beams.lightAlpha(NodeBeams.RingLight.LIT));
        assertEquals(NodeBeams.DIM_ALPHA, beams.lightAlpha(NodeBeams.RingLight.DIM));
        assertEquals(NodeBeams.DULL_ALPHA, beams.lightAlpha(NodeBeams.RingLight.DULL));
    }
}
