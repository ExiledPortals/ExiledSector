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

class MoveSlowSupplyUseTest {

    private static final float EPSILON = 1e-4f;

    private static FleetMemberAPI member(float contribution, boolean mothballed, MutableStat suppliesPerMonth) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.MOVE_SLOW_SUPPLY_USE_KEY, 0f)).thenReturn(contribution);
        when(member.getStats().getSuppliesPerMonth()).thenReturn(suppliesPerMonth);
        return member;
    }

    private static CampaignFleetAPI fleet(float currentBurnLevel, List<FleetMemberAPI> members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        when(fleet.getFleetData().getMinBurnLevel()).thenReturn(4f);
        StatBonus moveSlowBonus = new StatBonus();
        moveSlowBonus.modifyFlat("test", 5f);
        when(fleet.getStats().getDynamic().getMod("move_slow_speed_bonus_mod")).thenReturn(moveSlowBonus);
        when(fleet.getCurrBurnLevel()).thenReturn(currentBurnLevel);
        return fleet;
    }

    @Test
    void onlyTheStrongestActiveShipCounts() {
        List<FleetMemberAPI> members = List.of(member(-3f, false, new MutableStat(1f)), member(-5f, false, new MutableStat(1f)),
                member(-20f, true, new MutableStat(1f)), member(0f, false, new MutableStat(1f)));

        assertEquals(-5f, FleetWideEffects.strongestMoveSlowSupplyUse(members));
    }

    @Test
    void everyShipUsesLessSuppliesWhileTheFleetMovesSlowlyAndNormalAmountsOtherwise() {
        MutableStat carrierSupplies = new MutableStat(10f);
        MutableStat escortSupplies = new MutableStat(4f);
        List<FleetMemberAPI> members = List.of(member(-5f, false, carrierSupplies), member(0f, false, escortSupplies));

        FleetWideEffects.applyMoveSlowSupplyUse(fleet(1f, members));

        assertEquals(9.5f, carrierSupplies.getModifiedValue(), EPSILON);
        assertEquals(3.8f, escortSupplies.getModifiedValue(), EPSILON);

        FleetWideEffects.applyMoveSlowSupplyUse(fleet(20f, members));

        assertEquals(10f, carrierSupplies.getModifiedValue(), EPSILON);
        assertEquals(4f, escortSupplies.getModifiedValue(), EPSILON);
    }

    @Test
    void theEffectStoresItsValueForTheFleetCalculation() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        StatBonus contribution = new StatBonus();
        when(stats.getDynamic().getMod(FleetWideEffects.MOVE_SLOW_SUPPLY_USE_KEY)).thenReturn(contribution);

        LogisticsSkillEffect.MOVE_SLOW_SUPPLY_USE_MULT.apply(stats, "mod_id", -4f);

        assertEquals(-4f, contribution.getFlatBonus());
    }
}
