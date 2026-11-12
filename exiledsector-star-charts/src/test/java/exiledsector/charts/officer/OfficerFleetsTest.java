package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(12, OfficerFleets.recordIdOf("12"));
        assertNull(OfficerFleets.recordIdOf(null));
        assertNull(OfficerFleets.recordIdOf("officer twelve"));
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
}
