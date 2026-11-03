package exiledsector.loot;

import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.FleetMemberData;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncounterSalvageTest {

    private static FleetMemberData casualty(FleetMemberAPI member, FleetEncounterContextPlugin.Status status) {
        FleetMemberData data = mock(FleetMemberData.class);
        when(data.getMember()).thenReturn(member);
        when(data.getStatus()).thenReturn(status);
        return data;
    }

    @Test
    void theEncounterGivesThePlayersShareAndTheEnemyShipsTheyRecovered() {
        FleetMemberAPI recovered = mock(FleetMemberAPI.class);
        FleetMemberAPI destroyed = mock(FleetMemberAPI.class);
        List<FleetMemberData> casualties = List.of(casualty(recovered, FleetEncounterContextPlugin.Status.REPAIRED),
                casualty(destroyed, FleetEncounterContextPlugin.Status.DESTROYED));
        DataForEncounterSide winner = mock(DataForEncounterSide.class);
        when(winner.getEnemyCasualties()).thenReturn(casualties);
        FleetEncounterContextPlugin encounter = mock(FleetEncounterContextPlugin.class);
        when(encounter.getWinnerData()).thenReturn(winner);
        when(encounter.computePlayerContribFraction()).thenReturn(0.4f);

        EncounterSalvage salvage = EncounterSalvage.of(encounter);

        assertEquals(0.4f, salvage.playerShare());
        assertTrue(salvage.recovered(recovered));
        assertFalse(salvage.recovered(destroyed));
        assertEquals(EncounterSalvage.FULL, EncounterSalvage.of(null));
    }
}
