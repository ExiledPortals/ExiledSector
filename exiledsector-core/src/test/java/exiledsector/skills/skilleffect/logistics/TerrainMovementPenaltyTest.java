package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerrainMovementPenaltyTest {

    private static FleetMemberAPI member(float contribution, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.TERRAIN_MOVEMENT_PENALTY_KEY, 0f)).thenReturn(contribution);
        return member;
    }

    private static CampaignFleetAPI fleet(List<FleetMemberAPI> members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    @Test
    void eachActiveShipAddsItsReductionAndMothballedShipsAddNothing() {
        assertEquals(-10f, FleetWideEffects.terrainMovementPenaltyPercent(List.of(member(-5f, false), member(-5f, false), member(-5f, true),
                member(0f, false))));
    }

    @Test
    void theFleetReductionStopsAtTheMaximum() {
        List<FleetMemberAPI> members = List.of(member(-20f, false), member(-20f, false), member(-20f, false));

        assertEquals(-FleetWideEffects.MAX_TERRAIN_MOVEMENT_PENALTY_REDUCTION_PERCENT, FleetWideEffects.terrainMovementPenaltyPercent(members));
    }

    @Test
    void theReductionLowersTheCommandersNavigationPenaltyMultiplier() {
        CampaignFleetAPI fleet = fleet(List.of(member(-5f, false), member(-5f, false), member(-5f, false)));
        MutableStat penaltyMult = fleet.getCommanderStats().getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT);

        FleetWideEffects.applyTerrainMovementPenalty(fleet);

        verify(penaltyMult).modifyFlat(anyString(), eq(-0.15f), any());
    }

    @Test
    void aFleetWithoutTheEffectClearsItsModifier() {
        CampaignFleetAPI fleet = fleet(List.of(member(0f, false)));
        MutableStat penaltyMult = fleet.getCommanderStats().getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT);

        FleetWideEffects.applyTerrainMovementPenalty(fleet);

        verify(penaltyMult, never()).modifyFlat(anyString(), anyFloat(), any());
        verify(penaltyMult).unmodifyFlat(anyString());
    }
}
