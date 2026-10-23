package exiledlib.skills.skilleffect.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogisticsSkillEffectTest {

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
