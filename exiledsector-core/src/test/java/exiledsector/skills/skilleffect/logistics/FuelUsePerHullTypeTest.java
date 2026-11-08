package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FuelUsePerHullTypeTest {

    private static final float EPSILON = 1e-4f;

    private static FleetMemberAPI member(String baseHullId, float fuelContribution, float supplyContribution, boolean mothballed,
                                         StatBonus fuelUse) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getHullSpec().getBaseHullId()).thenReturn(baseHullId);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.FUEL_USE_PER_HULL_TYPE_KEY, 0f)).thenReturn(fuelContribution);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.SUPPLY_USE_PER_HULL_TYPE_KEY, 0f)).thenReturn(supplyContribution);
        when(member.getStats().getFuelUseMod()).thenReturn(fuelUse);
        return member;
    }

    private static FleetMemberAPI member(String baseHullId, float fuelContribution) {
        return member(baseHullId, fuelContribution, 0f, false, new StatBonus());
    }

    private static CampaignFleetAPI fleet(boolean playerFleet, List<FleetMemberAPI> members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.isPlayerFleet()).thenReturn(playerFleet);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    @Test
    void fuelAndSupplyReductionsCountTheSameActiveBaseHulls() {
        List<FleetMemberAPI> members = List.of(member("eagle", 1f, 3f, false, new StatBonus()), member("eagle", 0f),
                member("falcon", 0f), member("onslaught", 0f, 0f, true, new StatBonus()));

        assertEquals(2f, FleetWideEffects.fuelUsePerHullTypeReduction(members), EPSILON);
        assertEquals(6f, FleetWideEffects.supplyUsePerHullTypeReduction(members), EPSILON);
    }

    @Test
    void theFuelReductionIsCapped() {
        List<FleetMemberAPI> members = List.of(member("eagle", 1f), member("falcon", 1f), member("hammerhead", 0f), member("lasher", 0f));

        assertEquals(FleetWideEffects.MAX_FUEL_USE_PER_HULL_TYPE_REDUCTION, FleetWideEffects.fuelUsePerHullTypeReduction(members), EPSILON);
    }

    @Test
    void everyPlayerShipUsesLessFuelAndTheReductionClearsOnceTheContributionIsGone() {
        StatBonus eagleFuelUse = new StatBonus();
        StatBonus falconFuelUse = new StatBonus();
        FleetMemberAPI falcon = member("falcon", 0f, 0f, false, falconFuelUse);

        FleetWideEffects.applyFuelUsePerHullType(fleet(true, List.of(member("eagle", 1f, 0f, false, eagleFuelUse), falcon)));

        assertEquals(0.98f, eagleFuelUse.computeEffective(1f), EPSILON);
        assertEquals(0.98f, falconFuelUse.computeEffective(1f), EPSILON);

        FleetWideEffects.applyFuelUsePerHullType(fleet(true, List.of(member("eagle", 0f, 0f, false, eagleFuelUse), falcon)));

        assertEquals(1f, eagleFuelUse.computeEffective(1f), EPSILON);
        assertEquals(1f, falconFuelUse.computeEffective(1f), EPSILON);
    }

    @Test
    void otherFleetsAreLeftAlone() {
        StatBonus fuelUse = new StatBonus();

        FleetWideEffects.applyFuelUsePerHullType(fleet(false, List.of(member("eagle", 1f, 0f, false, fuelUse), member("falcon", 0f))));

        assertEquals(1f, fuelUse.computeEffective(1f), EPSILON);
    }

    @Test
    void theEffectStoresItsValueForTheFleetCalculation() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        StatBonus contribution = new StatBonus();
        when(stats.getDynamic().getMod(FleetWideEffects.FUEL_USE_PER_HULL_TYPE_KEY)).thenReturn(contribution);

        LogisticsSkillEffect.FUEL_USE_REDUCTION_PERCENT_PER_HULL_TYPE.apply(stats, "mod_id", 1f);

        assertEquals(1f, contribution.getFlatBonus());
    }
}
