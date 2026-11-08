package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSector;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransponderOffBurnLevelTest {

    private static final String FLEET_MOD_ID = ExiledSector.key("transponderOffBurnLevel");

    private static FleetMemberAPI member(float contribution, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.TRANSPONDER_OFF_BURN_LEVEL_KEY, 0f)).thenReturn(contribution);
        return member;
    }

    private static CampaignFleetAPI fleet(boolean transponderOn, StatBonus fleetBurnMod, FleetMemberAPI... members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.isTransponderOn()).thenReturn(transponderOn);
        when(fleet.getStats().getFleetwideMaxBurnMod()).thenReturn(fleetBurnMod);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(List.of(members));
        return fleet;
    }

    @Test
    void onlyTheStrongestActiveShipCounts() {
        assertEquals(2f, FleetWideEffects.transponderOffBurnLevelBonus(
                List.of(member(1f, false), member(2f, false), member(3f, true), member(0f, false))));
    }

    @Test
    void theBurnLevelRisesOnlyWhileTheTransponderIsOff() {
        StatBonus darkBurn = mock(StatBonus.class);
        StatBonus litBurn = mock(StatBonus.class);

        FleetWideEffects.applyTransponderOffBurnLevel(fleet(false, darkBurn, member(1f, false)));
        FleetWideEffects.applyTransponderOffBurnLevel(fleet(true, litBurn, member(1f, false)));

        verify(darkBurn).modifyFlat(eq(FLEET_MOD_ID), eq(1f), anyString());
        verify(litBurn).unmodifyFlat(FLEET_MOD_ID);
        verify(litBurn, never()).modifyFlat(anyString(), anyFloat(), anyString());
    }
}
