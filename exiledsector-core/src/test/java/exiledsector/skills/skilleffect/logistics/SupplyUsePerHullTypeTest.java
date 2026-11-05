package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplyUsePerHullTypeTest {

    private static final float EPSILON = 1e-4f;

    private static FleetMemberAPI member(String baseHullId, float contribution, boolean mothballed, MutableStat suppliesPerMonth) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getHullSpec().getBaseHullId()).thenReturn(baseHullId);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.SUPPLY_USE_PER_HULL_TYPE_KEY, 0f)).thenReturn(contribution);
        when(member.getStats().getSuppliesPerMonth()).thenReturn(suppliesPerMonth);
        return member;
    }

    private static FleetMemberAPI member(String baseHullId, float contribution) {
        return member(baseHullId, contribution, false, new MutableStat(1f));
    }

    private static CampaignFleetAPI fleet(boolean playerFleet, List<FleetMemberAPI> members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.isPlayerFleet()).thenReturn(playerFleet);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    @Test
    void eachDifferentActiveBaseHullAddsTheSummedContribution() {
        List<FleetMemberAPI> members = List.of(member("eagle", 3f), member("eagle", 0f), member("falcon", 0f), member("onslaught", 0f, true,
                new MutableStat(1f)));

        assertEquals(6f, FleetWideEffects.supplyUsePerHullTypeReduction(members), EPSILON);
    }

    @Test
    void severalContributingShipsAddUpToTheCap() {
        List<FleetMemberAPI> members = List.of(member("eagle", 3f), member("falcon", 3f), member("hammerhead", 0f), member("lasher", 0f));

        assertEquals(FleetWideEffects.MAX_SUPPLY_USE_PER_HULL_TYPE_REDUCTION, FleetWideEffects.supplyUsePerHullTypeReduction(members), EPSILON);
    }

    @Test
    void withoutAContributionThereIsNoReduction() {
        assertEquals(0f, FleetWideEffects.supplyUsePerHullTypeReduction(List.of(member("eagle", 0f), member("falcon", 0f))), EPSILON);
    }

    @Test
    void everyPlayerShipUsesLessSuppliesAndTheReductionClearsOnceTheContributionIsGone() {
        MutableStat eagleSupplies = new MutableStat(10f);
        MutableStat falconSupplies = new MutableStat(5f);
        FleetMemberAPI contributingEagle = member("eagle", 3f, false, eagleSupplies);
        FleetMemberAPI falcon = member("falcon", 0f, false, falconSupplies);

        FleetWideEffects.applySupplyUsePerHullType(fleet(true, List.of(contributingEagle, falcon)));

        assertEquals(9.4f, eagleSupplies.getModifiedValue(), EPSILON);
        assertEquals(4.7f, falconSupplies.getModifiedValue(), EPSILON);

        FleetWideEffects.applySupplyUsePerHullType(fleet(true, List.of(member("eagle", 0f, false, eagleSupplies), falcon)));

        assertEquals(10f, eagleSupplies.getModifiedValue(), EPSILON);
        assertEquals(5f, falconSupplies.getModifiedValue(), EPSILON);
    }

    @Test
    void otherFleetsAreLeftAlone() {
        MutableStat supplies = new MutableStat(10f);

        FleetWideEffects.applySupplyUsePerHullType(fleet(false, List.of(member("eagle", 3f, false, supplies), member("falcon", 0f))));

        assertEquals(10f, supplies.getModifiedValue(), EPSILON);
    }

    @Test
    void theEffectStoresItsValueForTheFleetCalculation() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        StatBonus contribution = new StatBonus();
        when(stats.getDynamic().getMod(FleetWideEffects.SUPPLY_USE_PER_HULL_TYPE_KEY)).thenReturn(contribution);

        LogisticsSkillEffect.SUPPLY_USE_REDUCTION_PERCENT_PER_HULL_TYPE.apply(stats, "mod_id", 3f);

        assertEquals(3f, contribution.getFlatBonus());
    }
}
