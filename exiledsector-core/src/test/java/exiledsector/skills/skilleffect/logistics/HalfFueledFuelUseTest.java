package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HalfFueledFuelUseTest {

    private static final float EPSILON = 1e-4f;

    private static FleetMemberAPI member(float contribution, StatBonus fuelUse) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.HALF_FUELED_FUEL_USE_KEY, 0f)).thenReturn(contribution);
        when(member.getStats().getFuelUseMod()).thenReturn(fuelUse);
        return member;
    }

    private static CampaignFleetAPI fleet(boolean playerFleet, float fuel, float maxFuel, List<FleetMemberAPI> members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.isPlayerFleet()).thenReturn(playerFleet);
        when(fleet.getCargo().getFuel()).thenReturn(fuel);
        when(fleet.getCargo().getMaxFuel()).thenReturn(maxFuel);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    @Test
    void halfOfTheFleetsFuelCapacityIsEnough() {
        assertTrue(FleetWideEffects.isHalfFueled(50f, 100f));
        assertFalse(FleetWideEffects.isHalfFueled(49f, 100f));
        assertFalse(FleetWideEffects.isHalfFueled(0f, 0f));
    }

    @Test
    void onlyTheShipWithTheEffectUsesLessFuelAndOnlyWhileTheTanksAreHalfFull() {
        StatBonus contributingFuelUse = new StatBonus();
        StatBonus otherFuelUse = new StatBonus();
        List<FleetMemberAPI> members = List.of(member(10f, contributingFuelUse), member(0f, otherFuelUse));

        FleetWideEffects.applyHalfFueledFuelUse(fleet(true, 60f, 100f, members));

        assertEquals(0.9f, contributingFuelUse.computeEffective(1f), EPSILON);
        assertEquals(1f, otherFuelUse.computeEffective(1f), EPSILON);

        FleetWideEffects.applyHalfFueledFuelUse(fleet(true, 40f, 100f, members));

        assertEquals(1f, contributingFuelUse.computeEffective(1f), EPSILON);
    }

    @Test
    void otherFleetsAreLeftAlone() {
        StatBonus fuelUse = new StatBonus();

        FleetWideEffects.applyHalfFueledFuelUse(fleet(false, 100f, 100f, List.of(member(10f, fuelUse))));

        assertEquals(1f, fuelUse.computeEffective(1f), EPSILON);
    }
}
