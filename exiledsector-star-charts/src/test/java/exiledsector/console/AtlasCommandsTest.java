package exiledsector.console;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AtlasCommandsTest {

    @Test
    void aTargetIsTwoCoordinatesOrASiteIdFollowedByTheRest() {
        assertEquals(new AtlasTargetCommand.Target(new AtlasCell(4, -7), null, List.of(), null), AtlasTargetCommand.Target.parse(" 4  -7 "));
        assertEquals(new AtlasTargetCommand.Target(new AtlasCell(0, 25), null, List.of("15"), null), AtlasTargetCommand.Target.parse("0 25 15"));
        assertEquals(new AtlasTargetCommand.Target(null, "bastion", List.of("tier=15"), null), AtlasTargetCommand.Target.parse("bastion tier=15"));
        assertNotNull(AtlasTargetCommand.Target.parse("").problem());
        assertNotNull(AtlasTargetCommand.Target.parse("4").problem());
        assertNotNull(AtlasTargetCommand.Target.parse("4 nexus").problem());
    }

    @Test
    void completeTakesAnOptionalTier() {
        assertEquals(1, AtlasCompleteCommand.tier(List.of()));
        assertEquals(15, AtlasCompleteCommand.tier(List.of("15")));
        assertEquals(7, AtlasCompleteCommand.tier(List.of("TIER=7")));
        assertNull(AtlasCompleteCommand.tier(List.of("16")));
        assertNull(AtlasCompleteCommand.tier(List.of("3", "4")));
    }

    @Test
    void grantAndResetArgumentsAreValidated() {
        assertEquals(new AtlasGrantCommand.GrantArguments(1, 0, null), AtlasGrantCommand.GrantArguments.parse(""));
        assertEquals(new AtlasGrantCommand.GrantArguments(0, 3, null), AtlasGrantCommand.GrantArguments.parse("0 3"));
        assertNotNull(AtlasGrantCommand.GrantArguments.parse("101").problem());
        assertNotNull(AtlasGrantCommand.GrantArguments.parse("1 2 3").problem());
        assertEquals(new AtlasResetCommand.ResetArguments(null, false, null), AtlasResetCommand.ResetArguments.parse(""));
        assertEquals(new AtlasResetCommand.ResetArguments(-5L, false, null), AtlasResetCommand.ResetArguments.parse("-5"));
        assertEquals(new AtlasResetCommand.ResetArguments(null, true, null), AtlasResetCommand.ResetArguments.parse("SAME"));
        assertNotNull(AtlasResetCommand.ResetArguments.parse("new").problem());
    }
}
