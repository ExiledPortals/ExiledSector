package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeaponAndMiscEffectsTest {

    private static final float EPSILON = 1e-4f;

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;
    private MutableShipStatsAPI stats;
    private DynamicStatsAPI dynamic;
    private ShipAPI ship;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        stats = mock(MutableShipStatsAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        ship = mock(ShipAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(ship.getMutableStats()).thenReturn(stats);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private StatBonus realDynamicMod(String key) {
        StatBonus bonus = new StatBonus();
        when(dynamic.getMod(key)).thenReturn(bonus);
        return bonus;
    }

    private void giveDMods(int count) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        HullModSpecAPI dmod = mock(HullModSpecAPI.class);
        when(dmod.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(settings.getHullModSpec(anyString())).thenReturn(dmod);
        when(variant.getHullMods()).thenReturn(IntStream.range(0, count).mapToObj(i -> "dmod_" + i).toList());
        when(stats.getVariant()).thenReturn(variant);
    }

    @Test
    void perDModWeaponDamageMultipliesEveryWeaponTypeOnceWithoutDoublingOnBeams() {
        giveDMods(2);
        MutableStat ballistic = new MutableStat(1f);
        MutableStat missile = new MutableStat(1f);
        MutableStat energy = new MutableStat(1f);
        MutableStat beam = new MutableStat(1f);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballistic);
        when(stats.getMissileWeaponDamageMult()).thenReturn(missile);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);

        WeaponSkillEffect.WEAPON_DAMAGE_MULT_PER_DMOD.apply(stats, "mod_id", 10f);

        assertEquals(1.21f, ballistic.getModifiedValue(), EPSILON);
        assertEquals(1.21f, missile.getModifiedValue(), EPSILON);
        assertEquals(1.21f, energy.getModifiedValue(), EPSILON);
        assertEquals(1f, beam.getModifiedValue(), EPSILON);
    }

    private MutableStat ballisticDamageWithBurnLevel(float baseBurn, float burnChange) {
        MutableStat burn = new MutableStat(baseBurn);
        burn.modifyFlat("other", burnChange);
        MutableStat ballistic = new MutableStat(1f);
        when(stats.getMaxBurnLevel()).thenReturn(burn);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballistic);
        WeaponSkillEffect.BALLISTIC_WEAPON_DAMAGE_PER_BURN_LEVEL_MULT.apply(stats, "mod_id", 5f);
        WeaponSkillEffect.BALLISTIC_WEAPON_DAMAGE_PER_BURN_LEVEL_MULT.applyAfterShipCreation(ship, "mod_id", 5f);
        return ballistic;
    }

    @Test
    void ballisticDamageGrowsWithEachBurnLevelAboveTheHullsDefault() {
        assertEquals(1.1f, ballisticDamageWithBurnLevel(9f, 2f).getModifiedValue(), EPSILON);
    }

    @Test
    void aBurnLevelBelowTheHullsDefaultNeverReducesBallisticDamage() {
        assertEquals(1f, ballisticDamageWithBurnLevel(9f, -3f).getModifiedValue(), EPSILON);
    }

    @Test
    void energyRangeGrowsWithSensorStrengthOnEnergyWeaponsOnly() {
        StatBonus energyRange = new StatBonus();
        StatBonus beamRange = new StatBonus();
        StatBonus ballisticRange = new StatBonus();
        when(stats.getSensorStrength()).thenReturn(new MutableStat(120f));
        when(stats.getEnergyWeaponRangeBonus()).thenReturn(energyRange);
        when(stats.getBeamWeaponRangeBonus()).thenReturn(beamRange);
        when(stats.getBallisticWeaponRangeBonus()).thenReturn(ballisticRange);

        WeaponSkillEffect.ENERGY_WEAPON_RANGE_PER_SENSOR_STRENGTH_FLAT.applyAfterShipCreation(ship, "mod_id", 0.5f);

        assertEquals(60f, energyRange.getFlatBonus(), EPSILON);
        assertEquals(0f, beamRange.getFlatBonus(), EPSILON);
        assertEquals(0f, ballisticRange.getFlatBonus(), EPSILON);
    }

    @Test
    void largeBallisticOpCostUsesTheKeyVanillasHeavyBallisticsIntegrationReads() {
        StatBonus largeBallistic = realDynamicMod("large_ballistic_mod");

        WeaponSkillEffect.BALLISTIC_WEAPON_LARGE_OP_COST_FLAT.apply(stats, "mod_id", -5f);

        assertEquals(-5f, largeBallistic.getFlatBonus(), EPSILON);
        assertTrue(WeaponSkillEffect.BALLISTIC_WEAPON_LARGE_OP_COST_FLAT.lowerIsBetter());
    }

    @Test
    void pdFlagsSwitchTheVanillaBehaviourOnWhateverTheMagnitude() {
        StatBonus ignoresFlares = realDynamicMod(Stats.PD_IGNORES_FLARES);
        StatBonus bestTargetLeading = realDynamicMod(Stats.PD_BEST_TARGET_LEADING);

        MiscSkillEffect.PD_IGNORES_DECOY_FLARES.apply(stats, "mod_id", 7f);
        MiscSkillEffect.PD_BEST_TARGET_LEADING.apply(stats, "mod_id", 0.5f);

        assertEquals(1f, ignoresFlares.getFlatBonus(), EPSILON);
        assertEquals(1f, bestTargetLeading.getFlatBonus(), EPSILON);
        assertFalse(MiscSkillEffect.PD_IGNORES_DECOY_FLARES.supportsTemporaryGating());
        assertFalse(MiscSkillEffect.PD_BEST_TARGET_LEADING.supportsTemporaryGating());
    }

    private static WeaponAPI weapon(WeaponSize size, WeaponType type, boolean strike) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getSize()).thenReturn(size);
        when(weapon.getType()).thenReturn(type);
        when(weapon.hasAIHint(AIHints.STRIKE)).thenReturn(strike);
        return weapon;
    }

    @Test
    void onlySmallNonMissileNonStrikeWeaponsBecomePointDefence() {
        WeaponAPI smallBallistic = weapon(WeaponSize.SMALL, WeaponType.BALLISTIC, false);
        WeaponAPI smallEnergy = weapon(WeaponSize.SMALL, WeaponType.ENERGY, false);
        WeaponAPI smallMissile = weapon(WeaponSize.SMALL, WeaponType.MISSILE, false);
        WeaponAPI smallStrike = weapon(WeaponSize.SMALL, WeaponType.ENERGY, true);
        WeaponAPI mediumBallistic = weapon(WeaponSize.MEDIUM, WeaponType.BALLISTIC, false);
        when(ship.getAllWeapons()).thenReturn(List.of(smallBallistic, smallEnergy, smallMissile, smallStrike, mediumBallistic));

        MiscSkillEffect.PD_RECLASSIFY_SMALL_WEAPONS.applyAfterShipCreation(ship, "mod_id", 1f);

        verify(smallBallistic).setPD(true);
        verify(smallEnergy).setPD(true);
        verify(smallMissile, never()).setPD(anyBoolean());
        verify(smallStrike, never()).setPD(anyBoolean());
        verify(mediumBallistic, never()).setPD(anyBoolean());
        assertFalse(MiscSkillEffect.PD_RECLASSIFY_SMALL_WEAPONS.supportsTemporaryGating());
    }

    @Test
    void objectiveCaptureRateIsAMultiplierOnTheVanillaStat() {
        MutableStat captureRate = new MutableStat(1f);
        when(dynamic.getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT)).thenReturn(captureRate);

        MiscSkillEffect.OBJECTIVE_CAPTURE_RATE_MULT.apply(stats, "mod_id", 50f);

        assertEquals(1.5f, captureRate.getModifiedValue(), EPSILON);
    }

    private float commandPointRecoveryFor(FleetMemberAPI member, PersonAPI captain) {
        StatBonus commandPointRate = realDynamicMod("command_point_rate_flat");
        when(stats.getFleetMember()).thenReturn(member);
        when(ship.getCaptain()).thenReturn(captain);
        MiscSkillEffect.COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP.advanceInCombat(ship, "mod_id", 250f);
        return commandPointRate.getFlatBonus();
    }

    @Test
    void anNpcShipCaptainedByItsFleetsCommanderForStatsCountsAsTheFlagship() {
        PersonAPI admiral = mock(PersonAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getFleetCommanderForStats()).thenReturn(admiral);

        assertEquals(2.5f, commandPointRecoveryFor(member, admiral), EPSILON);
    }

    @Test
    void theFleetCommanderIsUsedWhenThereIsNoCommanderForStats() {
        PersonAPI admiral = mock(PersonAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getFleetCommander()).thenReturn(admiral);

        assertEquals(2.5f, commandPointRecoveryFor(member, admiral), EPSILON);
    }

    @Test
    void aShipCaptainedBySomeoneElseOrWithNoFleetMemberIsNotTheFlagship() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        PersonAPI admiral = mock(PersonAPI.class);
        when(member.getFleetCommanderForStats()).thenReturn(admiral);

        assertEquals(0f, commandPointRecoveryFor(member, mock(PersonAPI.class)), EPSILON);
        assertEquals(0f, commandPointRecoveryFor(null, mock(PersonAPI.class)), EPSILON);
    }
}
