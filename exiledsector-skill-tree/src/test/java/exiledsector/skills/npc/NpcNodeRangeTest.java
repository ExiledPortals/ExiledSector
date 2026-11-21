package exiledsector.skills.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class NpcNodeRangeTest {

    @Test
    void extraLevelsRaiseBothEndsUpToTheNodeCap() {
        NpcNodeRange range = new NpcNodeRange(10, 20);

        assertEquals(new NpcNodeRange(14, 24), range.raisedBy(4, 100));
        assertEquals(new NpcNodeRange(18, 22), range.raisedBy(8, 22));
    }

    @Test
    void noExtraLevelsKeepTheRangeAndTheCapNeverLowersIt() {
        NpcNodeRange range = new NpcNodeRange(10, 20);

        assertSame(range, range.raisedBy(0, 100));
        assertSame(range, range.raisedBy(-3, 100));
        assertEquals(range, range.raisedBy(5, 8));
    }
}
