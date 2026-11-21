package exiledsector.console;

import exiledsector.charts.ChartTables;
import exiledsector.charts.officer.scar.ScarTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantScarsCommandTest {

    @BeforeEach
    void loadScars() throws Exception {
        ScarTable.register(ChartTables.csvRows("data/config/exiledSector/atlas_officer_scars.csv"));
    }

    @Test
    void scarsWithAndWithoutStacksAreRead() {
        GrantScarsCommand.GrantArguments arguments = GrantScarsCommand.GrantArguments.parse("rammed:2 Kited rammed");
        assertNull(arguments.problem());
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("rammed", 3);
        expected.put("kited", 1);
        assertEquals(expected, arguments.scars());
        assertFalse(arguments.onPlayer());
    }

    @Test
    void playerTargetsThePlayersFlagship() {
        GrantScarsCommand.GrantArguments arguments = GrantScarsCommand.GrantArguments.parse("player overloaded");
        assertTrue(arguments.onPlayer());
        assertEquals(Map.of("overloaded", 1), arguments.scars());
    }

    @Test
    void noneClearsTheScars() {
        GrantScarsCommand.GrantArguments arguments = GrantScarsCommand.GrantArguments.parse("none");
        assertNull(arguments.problem());
        assertTrue(arguments.scars().isEmpty());
    }

    @Test
    void unknownScarsBadStacksAndNoScarsAreProblems() {
        assertNotNull(GrantScarsCommand.GrantArguments.parse("no_such_scar").problem());
        assertNotNull(GrantScarsCommand.GrantArguments.parse("rammed:0").problem());
        assertNotNull(GrantScarsCommand.GrantArguments.parse("rammed:lots").problem());
        assertNotNull(GrantScarsCommand.GrantArguments.parse("player").problem());
        assertNotNull(GrantScarsCommand.GrantArguments.parse("").problem());
    }

    @Test
    void theDescriptionListsScarNamesAndStacks() {
        Map<String, Integer> scars = new LinkedHashMap<>();
        scars.put("rammed", 2);
        scars.put("flux_pinned", 1);
        assertEquals("Rammed x2, Flux-Pinned x1", GrantScarsCommand.describe(scars));
        assertEquals("none", GrantScarsCommand.describe(Map.of()));
    }
}
