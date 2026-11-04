package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.i18n.HeadlessGameText;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetCoronaResistanceTest {

    private static FleetMemberAPI member(float contribution, boolean mothballed, MutableStat coronaEffect) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.CORONA_RESISTANCE_CONTRIBUTION_KEY, 0f)).thenReturn(contribution);
        when(member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT)).thenReturn(coronaEffect);
        return member;
    }

    private static CampaignFleetAPI fleet(List<FleetMemberAPI> members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    @Test
    void activeCarriersAddUpAndTheFleetIsImmuneAtOneHundredPercent() {
        assertEquals(10f, FleetWideEffects.coronaResistancePercent(List.of(member(5f, false, null), member(5f, false, null),
                member(5f, true, null), member(0f, false, null))));
        assertEquals(FleetWideEffects.MAX_CORONA_RESISTANCE_PERCENT, FleetWideEffects.coronaResistancePercent(List.of(member(60f, false, null),
                member(60f, false, null))));
    }

    @Test
    void everyShipInTheFleetTakesTheFleetTotalAsOneMultiplier() throws Exception {
        MutableStat carrierEffect = new MutableStat(1f);
        MutableStat escortEffect = new MutableStat(1f);
        escortEffect.modifyMult("solar_shielding", 0.5f);

        try (HeadlessGameText ignored = HeadlessGameText.install(() -> false)) {
            FleetWideEffects.applyCoronaResistance(fleet(List.of(member(5f, false, carrierEffect), member(5f, false, escortEffect))));
        }

        assertEquals(0.9f, carrierEffect.getModifiedValue(), 0.0001f);
        assertEquals(0.45f, escortEffect.getModifiedValue(), 0.0001f);
    }

    @Test
    void aFleetWithoutCarriersIsLeftAlone() {
        MutableStat coronaEffect = new MutableStat(1f);

        FleetWideEffects.applyCoronaResistance(fleet(List.of(member(0f, false, coronaEffect))));

        assertEquals(1f, coronaEffect.getModifiedValue());
    }
}
