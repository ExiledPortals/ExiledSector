package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.officer.scar.ScarMemory;
import exiledsector.charts.officer.scar.Scars;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfficerFleetsTest {

    private static FleetMemberAPI member(String memberId) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(memberId);
        return member;
    }

    private static MemoryAPI fleetMemory(String flagshipId, String treeSeed) {
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getString(OfficerFleets.FLAGSHIP_MEMORY_KEY)).thenReturn(flagshipId);
        when(memory.getString(OfficerFleets.TREE_SEED_MEMORY_KEY)).thenReturn(treeSeed);
        return memory;
    }

    @Test
    void aRunNodeNamesItsOfficerByRecordId() {
        assertEquals(12, OfficerFleets.recordIdOfRunData("12"));
        assertNull(OfficerFleets.recordIdOfRunData(null));
        assertNull(OfficerFleets.recordIdOfRunData("officer twelve"));
    }

    @Test
    void onlyTheOfficersFlagshipGetsTheOfficersTreeSeed() {
        MemoryAPI memory = fleetMemory("flagship-1", "abc123");

        assertEquals(OfficerFleets.TREE_SEED_PREFIX + "abc123", OfficerFleets.flagshipTreeSeed(memory, member("flagship-1")));
        assertNull(OfficerFleets.flagshipTreeSeed(memory, member("escort-2")));
    }

    @Test
    void aFleetWithoutAnOfficerFlagshipGivesNoTreeSeed() {
        assertNull(OfficerFleets.flagshipTreeSeed(fleetMemory(null, null), member("flagship-1")));
        assertNull(OfficerFleets.flagshipTreeSeed(fleetMemory("flagship-1", null), member("flagship-1")));
        assertNull(OfficerFleets.flagshipTreeSeed(null, member("flagship-1")));
        assertNull(OfficerFleets.flagshipTreeSeed(fleetMemory("flagship-1", "abc123"), null));
    }

    private static MemoryAPI scarMemory(String flagshipId, String encodedScars) {
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.get(ScarMemory.FLAGSHIP_ID_KEY)).thenReturn(flagshipId);
        when(memory.get(ScarMemory.SCARS_KEY)).thenReturn(encodedScars);
        return memory;
    }

    @Test
    void onlyTheScarredFlagshipGainsTheScarsExtraLevels() {
        MemoryAPI memory = scarMemory("flagship-1", "veteran:2");

        int levels = OfficerFleets.flagshipExtraLevels(memory, member("flagship-1"));

        assertEquals(Scars.extraLevels(Map.of("veteran", 2)), levels);
        assertTrue(levels > 0);
        assertEquals(0, OfficerFleets.flagshipExtraLevels(memory, member("escort-2")));
    }

    @Test
    void anUnscarredOrMissingFlagshipGainsNoLevels() {
        assertEquals(0, OfficerFleets.flagshipExtraLevels(scarMemory("flagship-1", null), member("flagship-1")));
        assertEquals(0, OfficerFleets.flagshipExtraLevels(scarMemory(null, "veteran:1"), member("flagship-1")));
        assertEquals(0, OfficerFleets.flagshipExtraLevels(null, member("flagship-1")));
        assertEquals(0, OfficerFleets.flagshipExtraLevels(scarMemory("flagship-1", "veteran:1"), null));
    }
}
