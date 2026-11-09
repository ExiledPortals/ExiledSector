package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.skilleffect.StatMode;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetContributionsTest {

    private static final String KEY = "contribution";

    private static FleetMemberAPI member(float contribution, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(KEY, 0f)).thenReturn(contribution);
        return member;
    }

    @Test
    void activeShipsAddTheirContributionsAndMothballedShipsAddNothing() {
        assertEquals(-3f, FleetContributions.sum(List.of(member(4f, false), member(-7f, false), member(100f, true)), KEY), 0.001f);
        assertEquals(0f, FleetContributions.sum(List.of(), KEY));
    }

    @Test
    void theLargestPositiveContributionIgnoresNegativesAndMothballedShips() {
        assertEquals(4f, FleetContributions.largestPositive(List.of(member(4f, false), member(-7f, false), member(100f, true)), KEY), 0.001f);
        assertEquals(0f, FleetContributions.largestPositive(List.of(member(-7f, false)), KEY));
    }

    @Test
    void theLargestMagnitudeContributionKeepsItsSign() {
        assertEquals(-7f, FleetContributions.largestMagnitude(List.of(member(4f, false), member(-7f, false), member(-100f, true)), KEY), 0.001f);
        assertEquals(0f, FleetContributions.largestMagnitude(List.of(), KEY));
    }

    @Test
    void contributionEffectsReportTheirModeAndWhetherNpcShipsCarryThem() {
        assertFalse(LogisticsSkillEffect.POST_BATTLE_WEAPON_SALVAGE_PERCENT.appliesToNpcShips());
        assertFalse(LogisticsSkillEffect.RECOVERED_SHIP_DMOD_AVOID_CHANCE_PERCENT.appliesToNpcShips());
        assertTrue(LogisticsSkillEffect.FLEET_CORONA_RESISTANCE_MULT.appliesToNpcShips());
        assertNull(LogisticsSkillEffect.FLEET_CORONA_RESISTANCE_MULT.statMode());
        assertNull(LogisticsSkillEffect.BLACK_HOLE_HULL_ITEM_PERCENT.statMode());
        assertEquals(StatMode.MULT, LogisticsSkillEffect.MOVE_SLOW_SUPPLY_USE_MULT.statMode());
        assertTrue(LogisticsSkillEffect.MOVE_SLOW_SUPPLY_USE_MULT.isMultiplicative());
        assertTrue(LogisticsSkillEffect.MOVE_SLOW_SUPPLY_USE_MULT.lowerIsBetter());
        assertFalse(LogisticsSkillEffect.MOVE_SLOW_SUPPLY_USE_MULT.appliesToNpcShips());
    }
}
