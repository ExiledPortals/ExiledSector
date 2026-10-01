package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.skills.ShipFacts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FighterAndLogisticsEffectsTest {

    private static final float EPSILON = 1e-4f;

    private MutableShipStatsAPI stats;
    private DynamicStatsAPI dynamic;

    @BeforeEach
    void setUp() {
        stats = mock(MutableShipStatsAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
    }

    private StatBonus realDynamicMod(String key) {
        StatBonus bonus = new StatBonus();
        when(dynamic.getMod(key)).thenReturn(bonus);
        return bonus;
    }

    @Test
    void replacementRateMultDividesBothDecayAndRecoverySoTheRateDropsSlower() {
        MutableStat decay = new MutableStat(1f);
        MutableStat recovery = new MutableStat(1f);
        when(dynamic.getStat("replacement_rate_decrease_mult")).thenReturn(decay);
        when(dynamic.getStat("replacement_rate_increase_mult")).thenReturn(recovery);

        FighterSkillEffect.FIGHTER_REPLACEMENT_RATE_MULT.apply(stats, "mod_id", 25f);

        assertEquals(0.8f, decay.getModifiedValue(), EPSILON);
        assertEquals(0.8f, recovery.getModifiedValue(), EPSILON);
        assertFalse(FighterSkillEffect.FIGHTER_REPLACEMENT_RATE_MULT.supportsTemporaryGating());
    }

    @Test
    void relaunchTimeAddsAFractionOfTheBaseRefitTime() {
        StatBonus extraRearm = realDynamicMod("fighter_rearm_time_extra_fraction_of_base_refit_time_mod");

        FighterSkillEffect.FIGHTER_RELAUNCH_TIME_FLAT.apply(stats, "mod_id", 50f);

        assertEquals(0.5f, extraRearm.getFlatBonus(), EPSILON);
        assertTrue(FighterSkillEffect.FIGHTER_RELAUNCH_TIME_FLAT.lowerIsBetter());
        assertTrue(FighterSkillEffect.FIGHTER_RELAUNCH_TIME_FLAT.supportsTemporaryGating());
    }

    private static ShipAPI fighter(WingRole role, boolean hasWing) {
        ShipAPI fighter = mock(ShipAPI.class);
        MutableShipStatsAPI fighterStats = mock(MutableShipStatsAPI.class);
        when(fighter.getMutableStats()).thenReturn(fighterStats);
        when(fighterStats.getMaxSpeed()).thenReturn(new MutableStat(100f));
        if (hasWing) {
            FighterWingAPI wing = mock(FighterWingAPI.class);
            when(wing.getRole()).thenReturn(role);
            when(fighter.getWing()).thenReturn(wing);
        }
        return fighter;
    }

    private static float topSpeedPercentAfter(FighterSkillEffect effect, ShipAPI fighter) {
        effect.applyToFighterSpawnedByShip(fighter, mock(ShipAPI.class), "mod_id", 10f);
        return fighter.getMutableStats().getMaxSpeed().getPercentMod();
    }

    @Test
    void roleEffectsOnlyReachFightersOfThatRole() {
        assertEquals(10f, topSpeedPercentAfter(FighterSkillEffect.INTERCEPTOR_ROLE_TOP_SPEED_PERCENT,
                fighter(WingRole.INTERCEPTOR, true)), EPSILON);
        assertEquals(0f, topSpeedPercentAfter(FighterSkillEffect.INTERCEPTOR_ROLE_TOP_SPEED_PERCENT,
                fighter(WingRole.BOMBER, true)), EPSILON);
    }

    @Test
    void assaultWingsAndWinglessFightersCountAsTheFighterRole() {
        for (ShipAPI fighter : List.of(fighter(WingRole.ASSAULT, true), fighter(null, true), fighter(null, false))) {
            assertEquals(10f, topSpeedPercentAfter(FighterSkillEffect.FIGHTER_ROLE_TOP_SPEED_PERCENT, fighter), EPSILON);
        }
        assertEquals(0f, topSpeedPercentAfter(FighterSkillEffect.SUPPORT_ROLE_TOP_SPEED_PERCENT,
                fighter(WingRole.ASSAULT, true)), EPSILON);
    }

    @Test
    void onlyTheParentShipStatEffectsCanBeGatedTemporarily() {
        assertTrue(FighterSkillEffect.FIGHTER_REFIT_TIME_MULT.supportsTemporaryGating());
        assertFalse(FighterSkillEffect.FIGHTER_CREW_LOSS_PERCENT.supportsTemporaryGating());
        assertFalse(FighterSkillEffect.FIGHTER_ROLE_TOP_SPEED_PERCENT.supportsTemporaryGating());
    }

    @Test
    void crewBasedGroundSupportAddsAShareOfMaxCrewOnTopOfTheNodesFlatGroundSupport() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getMaxCrew()).thenReturn(400f);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(stats.getVariant()).thenReturn(variant);
        StatBonus maxCrew = new StatBonus();
        maxCrew.modifyPercent("other", 50f);
        when(stats.getMaxCrewMod()).thenReturn(maxCrew);
        StatBonus groundSupport = realDynamicMod(Stats.FLEET_GROUND_SUPPORT);

        LogisticsSkillEffect.GROUND_SUPPORT_FLAT.apply(stats, "mod_id", 100f);
        LogisticsSkillEffect.GROUND_SUPPORT_PER_MAX_CREW_PERCENT.apply(stats, "mod_id", 25f);

        assertEquals(100f + 150f, groundSupport.getFlatBonus(), EPSILON);
        assertTrue(LogisticsSkillEffect.GROUND_SUPPORT_PER_MAX_CREW_PERCENT.appliesAfterOtherEffects());
        assertFalse(LogisticsSkillEffect.GROUND_SUPPORT_FLAT.appliesAfterOtherEffects());
    }

    @Test
    void crewBasedGroundSupportDoesNothingWithoutAHullSpec() {
        StatBonus groundSupport = realDynamicMod(Stats.FLEET_GROUND_SUPPORT);

        LogisticsSkillEffect.GROUND_SUPPORT_PER_MAX_CREW_PERCENT.apply(stats, "mod_id", 25f);

        assertEquals(0f, groundSupport.getFlatBonus(), EPSILON);
    }

    @Test
    void removingTheCivilianHullPenaltyStripsVanillasSensorModifiers() {
        MutableStat strength = mock(MutableStat.class);
        MutableStat profile = mock(MutableStat.class);
        when(stats.getSensorStrength()).thenReturn(strength);
        when(stats.getSensorProfile()).thenReturn(profile);

        LogisticsSkillEffect.REMOVE_CIVILIAN_HULL_PENALTY.apply(stats, "mod_id", 1f);

        verify(strength).unmodify("civgrade");
        verify(profile).unmodify("civgrade");
        assertFalse(LogisticsSkillEffect.REMOVE_CIVILIAN_HULL_PENALTY.supportsTemporaryGating());
    }

    @Test
    void civilianOnlyNodesAreBlockedOnMilitaryHulls() {
        ShipFacts civilian = new ShipFacts(ShipAPI.HullSize.FRIGATE, ShieldAPI.ShieldType.FRONT, false, 100f, HullMods.CIVGRADE::equals);
        ShipFacts military = new ShipFacts(ShipAPI.HullSize.FRIGATE, ShieldAPI.ShieldType.FRONT, false, 100f, hullModId -> false);

        assertNull(LogisticsSkillEffect.REQUIRES_CIVILIAN_GRADE_HULL.blockAllocationReason(civilian, ShieldAPI.ShieldType.FRONT));
        assertNotNull(LogisticsSkillEffect.REQUIRES_CIVILIAN_GRADE_HULL.blockAllocationReason(military, ShieldAPI.ShieldType.FRONT));
    }

    @Test
    void fleetWideContributionsAreStoredOnTheShipForThePlayerFleetOnly() {
        StatBonus salvage = realDynamicMod(FleetWideEffects.POST_BATTLE_SALVAGE_CONTRIBUTION_KEY);
        StatBonus phaseField = realDynamicMod(FleetWideEffects.PHASE_FIELD_CONTRIBUTION_KEY);

        LogisticsSkillEffect.POST_BATTLE_SALVAGE_PERCENT.apply(stats, "mod_id", 15f);
        LogisticsSkillEffect.PHASE_FIELD_CONTRIBUTION_PERCENT.apply(stats, "mod_id", 40f);

        assertEquals(15f, salvage.getFlatBonus(), EPSILON);
        assertEquals(40f, phaseField.getFlatBonus(), EPSILON);
        for (SkillEffect effect : List.of(LogisticsSkillEffect.POST_BATTLE_SALVAGE_PERCENT,
                LogisticsSkillEffect.PHASE_FIELD_CONTRIBUTION_PERCENT)) {
            assertFalse(effect.appliesToNpcShips(), effect.name());
            assertFalse(effect.supportsTemporaryGating(), effect.name());
        }
    }
}
