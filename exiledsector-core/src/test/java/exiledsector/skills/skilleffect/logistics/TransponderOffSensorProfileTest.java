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

class TransponderOffSensorProfileTest {

    private static final String FLEET_MOD_ID = ExiledSector.key("transponderOffSensorProfile");

    private static FleetMemberAPI member(float contribution, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.TRANSPONDER_OFF_SENSOR_PROFILE_KEY, 0f)).thenReturn(contribution);
        return member;
    }

    private static CampaignFleetAPI fleet(boolean transponderOn, StatBonus detectedRangeMod, FleetMemberAPI... members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.isTransponderOn()).thenReturn(transponderOn);
        when(fleet.getStats().getDetectedRangeMod()).thenReturn(detectedRangeMod);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(List.of(members));
        return fleet;
    }

    @Test
    void eachActiveShipAddsItsReductionAndMothballedShipsAddNothing() {
        assertEquals(-20f, FleetWideEffects.transponderOffSensorProfilePercent(
                List.of(member(-10f, false), member(-10f, false), member(-10f, true), member(0f, false))));
    }

    @Test
    void theFleetReductionStopsAtTheMaximum() {
        List<FleetMemberAPI> members = List.of(member(-10f, false), member(-10f, false), member(-10f, false), member(-10f, false));

        assertEquals(-FleetWideEffects.MAX_TRANSPONDER_OFF_SENSOR_PROFILE_REDUCTION, FleetWideEffects.transponderOffSensorProfilePercent(members));
    }

    @Test
    void theReductionAppliesOnlyWhileTheTransponderIsOff() {
        StatBonus darkProfile = mock(StatBonus.class);
        StatBonus litProfile = mock(StatBonus.class);

        FleetWideEffects.applyTransponderOffSensorProfile(fleet(false, darkProfile, member(-10f, false)));
        FleetWideEffects.applyTransponderOffSensorProfile(fleet(true, litProfile, member(-10f, false)));

        verify(darkProfile).modifyPercent(eq(FLEET_MOD_ID), eq(-10f), anyString());
        verify(litProfile).unmodifyPercent(FLEET_MOD_ID);
        verify(litProfile, never()).modifyPercent(anyString(), anyFloat(), anyString());
    }
}
