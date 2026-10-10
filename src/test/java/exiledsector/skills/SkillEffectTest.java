package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import exiledsector.skills.skilleffect.CombatSkillEffect;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.FighterSkillEffect;
import exiledsector.skills.skilleffect.FluxSkillEffect;
import exiledsector.skills.skilleffect.LogisticsSkillEffect;
import exiledsector.skills.skilleffect.MiscSkillEffect;
import exiledsector.skills.skilleffect.MovementSkillEffect;
import exiledsector.skills.skilleffect.PhaseSkillEffect;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.magiclib.util.MagicFakeBeam;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SkillEffectTest {

    @Test
    void hullModifiesTheHullBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus hullBonus = mock(StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullBonus);

        DefenseSkillEffect.HULL_PERCENT.apply(stats, "mod_id", 10f);

        verify(hullBonus).modifyPercent("mod_id", 10f);
    }

    @Test
    void hullFlatModifiesTheHullBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus hullBonus = mock(StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullBonus);

        DefenseSkillEffect.HULL_FLAT.apply(stats, "mod_id", 250f);

        verify(hullBonus).modifyFlat("mod_id", 250f);
    }

    @Test
    void hullFlatDescribesAFlatChangeToHullPoints() {
        assertEquals("Increases hull points by 250.", DefenseSkillEffect.HULL_FLAT.description(250f).plain());
    }

    @Test
    void hullMultModifiesTheHullBonusStatMultiplicatively() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus hullBonus = mock(StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullBonus);

        DefenseSkillEffect.HULL_MULT.apply(stats, "mod_id", 2f);

        verify(hullBonus).modifyMult("mod_id", 1.02f);
    }

    @Test
    void hullMultDescribesAMultiplicativeChangeToHullPoints() {
        assertEquals("2% more hull points.", DefenseSkillEffect.HULL_MULT.description(2f).plain());
    }

    @Test
    void armorModifiesTheArmorBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus armorBonus = mock(StatBonus.class);
        when(stats.getArmorBonus()).thenReturn(armorBonus);

        DefenseSkillEffect.ARMOR_PERCENT.apply(stats, "mod_id", 10f);

        verify(armorBonus).modifyPercent("mod_id", 10f);
    }

    @Test
    void fluxCapacityModifiesTheFluxCapacityStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat fluxCapacity = mock(MutableStat.class);
        when(stats.getFluxCapacity()).thenReturn(fluxCapacity);

        FluxSkillEffect.FLUX_CAPACITY_PERCENT.apply(stats, "mod_id", 1f);

        verify(fluxCapacity).modifyPercent("mod_id", 1f);
    }

    @Test
    void fluxDissipationModifiesTheFluxDissipationStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat fluxDissipation = mock(MutableStat.class);
        when(stats.getFluxDissipation()).thenReturn(fluxDissipation);

        FluxSkillEffect.FLUX_DISSIPATION_PERCENT.apply(stats, "mod_id", 10f);

        verify(fluxDissipation).modifyPercent("mod_id", 10f);
    }

    @Test
    void ballisticDamageModifiesOnlyTheBallisticDamageStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat ballistic = mock(MutableStat.class);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballistic);

        SkillEffect.byName("BALLISTIC_WEAPON_DAMAGE_PERCENT").apply(stats, "mod_id", 5f);

        verify(ballistic).modifyPercent("mod_id", 5f);
    }

    @Test
    void missileDamageModifiesOnlyTheMissileDamageStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat missile = mock(MutableStat.class);
        when(stats.getMissileWeaponDamageMult()).thenReturn(missile);

        SkillEffect.byName("MISSILE_WEAPON_DAMAGE_PERCENT").apply(stats, "mod_id", 5f);

        verify(missile).modifyPercent("mod_id", 5f);
    }

    @Test
    void nonBeamEnergyDamageRaisesEnergyDamageAndCancelsItForBeams() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat energy = mock(MutableStat.class);
        MutableStat beam = mock(MutableStat.class);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);

        SkillEffect.byName("NON_BEAM_ENERGY_WEAPON_DAMAGE_PERCENT").apply(stats, "mod_id", 5f);

        verify(energy).modifyPercent("mod_id", 5f);
        verify(beam).modifyPercent("mod_id_nonBeamOffset", -5f);
    }

    @Test
    void beamDamageModifiesOnlyTheBeamDamageStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat beam = mock(MutableStat.class);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);

        SkillEffect.byName("BEAM_WEAPON_DAMAGE_PERCENT").apply(stats, "mod_id", 5f);

        verify(beam).modifyPercent("mod_id", 5f);
    }

    @Test
    void energyDamageModifiesOnlyTheEnergyStatBecauseItAlreadyReachesBeams() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat energy = mock(MutableStat.class);
        MutableStat beam = mock(MutableStat.class);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);

        SkillEffect.byName("ENERGY_WEAPON_DAMAGE_PERCENT").apply(stats, "mod_id", 5f);

        verify(energy).modifyPercent("mod_id", 5f);
        verifyNoInteractions(beam);
    }

    @Test
    void allWeaponDamageModifiesEachTypeStatButNotTheBeamStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat ballistic = mock(MutableStat.class);
        MutableStat missile = mock(MutableStat.class);
        MutableStat energy = mock(MutableStat.class);
        MutableStat beam = mock(MutableStat.class);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballistic);
        when(stats.getMissileWeaponDamageMult()).thenReturn(missile);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);

        SkillEffect.byName("WEAPON_DAMAGE_PERCENT").apply(stats, "mod_id", 5f);

        verify(ballistic).modifyPercent("mod_id", 5f);
        verify(missile).modifyPercent("mod_id", 5f);
        verify(energy).modifyPercent("mod_id", 5f);
        verifyNoInteractions(beam);
    }

    @Test
    void describeFormatsAWholeNumberMagnitudeWithoutADecimal() {
        assertEquals("10% increased hull points.", DefenseSkillEffect.HULL_PERCENT.description(10f).plain());
    }

    @Test
    void describeFormatsAFractionalMagnitudeWithADecimal() {
        assertEquals("0.5% increased flux capacity.", FluxSkillEffect.FLUX_CAPACITY_PERCENT.description(0.5f).plain());
    }

    @Test
    void describeUsesTheUnqualifiedNameForTheAllWeaponsScope() {
        assertEquals("5% increased weapon damage.", SkillEffect.byName("WEAPON_DAMAGE_PERCENT").description(5f).plain());
    }

    @Test
    void fuelCapacityModifiesTheFuelModStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus fuelMod = mock(StatBonus.class);
        when(stats.getFuelMod()).thenReturn(fuelMod);

        LogisticsSkillEffect.FUEL_CAPACITY_PERCENT.apply(stats, "mod_id", 20f);

        verify(fuelMod).modifyPercent("mod_id", 20f);
    }

    @Test
    void cargoCapacityModifiesTheCargoModStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus cargoMod = mock(StatBonus.class);
        when(stats.getCargoMod()).thenReturn(cargoMod);

        LogisticsSkillEffect.CARGO_CAPACITY_PERCENT.apply(stats, "mod_id", 20f);

        verify(cargoMod).modifyPercent("mod_id", 20f);
    }

    @Test
    void crewCapacityModifiesTheMaxCrewModStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus maxCrewMod = mock(StatBonus.class);
        when(stats.getMaxCrewMod()).thenReturn(maxCrewMod);

        LogisticsSkillEffect.CREW_CAPACITY_PERCENT.apply(stats, "mod_id", 20f);

        verify(maxCrewMod).modifyPercent("mod_id", 20f);
    }

    @Test
    void sensorProfileModifiesTheSensorProfileStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat sensorProfile = mock(MutableStat.class);
        when(stats.getSensorProfile()).thenReturn(sensorProfile);

        LogisticsSkillEffect.SENSOR_PROFILE_PERCENT.apply(stats, "mod_id", -10f);

        verify(sensorProfile).modifyPercent("mod_id", -10f);
    }

    @Test
    void sensorStrengthModifiesTheSensorStrengthStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat sensorStrength = mock(MutableStat.class);
        when(stats.getSensorStrength()).thenReturn(sensorStrength);

        LogisticsSkillEffect.SENSOR_STRENGTH_PERCENT.apply(stats, "mod_id", 20f);

        verify(sensorStrength).modifyPercent("mod_id", 20f);
    }

    @Test
    void combatVisionModifiesTheSightRadiusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus sightRadius = mock(StatBonus.class);
        when(stats.getSightRadiusMod()).thenReturn(sightRadius);

        LogisticsSkillEffect.COMBAT_VISION.apply(stats, "mod_id", 1000f);

        verify(sightRadius).modifyFlat("mod_id", 1000f);
    }

    @Test
    void coronaResistanceDescribesItsMultiplierTheWayItAppliesIt() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        MutableStat corona = mock(MutableStat.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getStat(com.fs.starfarer.api.impl.campaign.ids.Stats.CORONA_EFFECT_MULT)).thenReturn(corona);

        LogisticsSkillEffect.CORONA_RESISTANCE_MULT.apply(stats, "mod_id", -25f);

        verify(corona).modifyMult("mod_id", 0.75f);
        assertEquals("25% less combat readiness loss from being in a solar corona or a deep hyperspace storm.",
                LogisticsSkillEffect.CORONA_RESISTANCE_MULT.description(-25f).plain());
        assertTrue(LogisticsSkillEffect.CORONA_RESISTANCE_MULT.lowerIsBetter());
    }

    @Test
    void ballisticWeaponRangeModifiesTheBallisticRangeBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus rangeBonus = mock(StatBonus.class);
        when(stats.getBallisticWeaponRangeBonus()).thenReturn(rangeBonus);

        SkillEffect.byName("BALLISTIC_WEAPON_RANGE_PERCENT").apply(stats, "mod_id", 15f);

        verify(rangeBonus).modifyPercent("mod_id", 15f);
    }

    @Test
    void energyWeaponRangeModifiesTheEnergyRangeBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus rangeBonus = mock(StatBonus.class);
        when(stats.getEnergyWeaponRangeBonus()).thenReturn(rangeBonus);

        SkillEffect.byName("ENERGY_WEAPON_RANGE_PERCENT").apply(stats, "mod_id", 15f);

        verify(rangeBonus).modifyPercent("mod_id", 15f);
    }

    @Test
    void beamWeaponRangeModifiesTheBeamRangeBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus rangeBonus = mock(StatBonus.class);
        when(stats.getBeamWeaponRangeBonus()).thenReturn(rangeBonus);

        SkillEffect.byName("BEAM_WEAPON_RANGE_PERCENT").apply(stats, "mod_id", 15f);

        verify(rangeBonus).modifyPercent("mod_id", 15f);
    }

    @Test
    void ballisticAmmoModifiesTheBallisticAmmoBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus ammoBonus = mock(StatBonus.class);
        when(stats.getBallisticAmmoBonus()).thenReturn(ammoBonus);

        SkillEffect.byName("BALLISTIC_WEAPON_AMMO_PERCENT").apply(stats, "mod_id", 20f);

        verify(ammoBonus).modifyPercent("mod_id", 20f);
    }

    @Test
    void energyAmmoModifiesTheEnergyAmmoBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus ammoBonus = mock(StatBonus.class);
        when(stats.getEnergyAmmoBonus()).thenReturn(ammoBonus);

        SkillEffect.byName("ENERGY_WEAPON_AMMO_PERCENT").apply(stats, "mod_id", 20f);

        verify(ammoBonus).modifyPercent("mod_id", 20f);
    }

    @Test
    void missileAmmoModifiesTheMissileAmmoBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus ammoBonus = mock(StatBonus.class);
        when(stats.getMissileAmmoBonus()).thenReturn(ammoBonus);

        SkillEffect.byName("MISSILE_WEAPON_AMMO_PERCENT").apply(stats, "mod_id", 25f);

        verify(ammoBonus).modifyPercent("mod_id", 25f);
    }

    @Test
    void weaponTurnRateModifiesBothTheNonBeamAndBeamTurnRateStats() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus turnRateBonus = mock(StatBonus.class);
        StatBonus beamTurnRateBonus = mock(StatBonus.class);
        when(stats.getWeaponTurnRateBonus()).thenReturn(turnRateBonus);
        when(stats.getBeamWeaponTurnRateBonus()).thenReturn(beamTurnRateBonus);

        SkillEffect.byName("WEAPON_TURN_RATE_PERCENT").apply(stats, "mod_id", 20f);

        verify(turnRateBonus).modifyPercent("mod_id", 20f);
        verify(beamTurnRateBonus).modifyPercent("mod_id", 20f);
    }

    @Test
    void shieldArcModifiesTheShieldArcBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus arcBonus = mock(StatBonus.class);
        when(stats.getShieldArcBonus()).thenReturn(arcBonus);

        ShieldSkillEffect.SHIELD_ARC_PERCENT.apply(stats, "mod_id", 20f);

        verify(arcBonus).modifyPercent("mod_id", 20f);
    }

    @Test
    void shieldUpkeepModifiesTheShieldUpkeepMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat upkeepMult = mock(MutableStat.class);
        when(stats.getShieldUpkeepMult()).thenReturn(upkeepMult);

        ShieldSkillEffect.SHIELD_UPKEEP_PERCENT.apply(stats, "mod_id", -15f);

        verify(upkeepMult).modifyPercent("mod_id", -15f);
    }

    @Test
    void shieldAbsorptionModifiesTheShieldAbsorptionMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat absorptionMult = mock(MutableStat.class);
        when(stats.getShieldAbsorptionMult()).thenReturn(absorptionMult);

        DefenseSkillEffect.SHIELD_ABSORPTION_PERCENT.apply(stats, "mod_id", -10f);

        verify(absorptionMult).modifyPercent("mod_id", -10f);
    }

    @Test
    void shieldTurnRateModifiesTheShieldTurnRateMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat turnRateMult = mock(MutableStat.class);
        when(stats.getShieldTurnRateMult()).thenReturn(turnRateMult);

        ShieldSkillEffect.SHIELD_TURN_RATE_PERCENT.apply(stats, "mod_id", 15f);

        verify(turnRateMult).modifyPercent("mod_id", 15f);
    }

    @Test
    void shieldRaiseRateModifiesTheShieldUnfoldRateMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat unfoldRateMult = mock(MutableStat.class);
        when(stats.getShieldUnfoldRateMult()).thenReturn(unfoldRateMult);

        ShieldSkillEffect.SHIELD_RAISE_RATE_PERCENT.apply(stats, "mod_id", 15f);

        verify(unfoldRateMult).modifyPercent("mod_id", 15f);
    }

    @Test
    void weaponDurabilityModifiesTheWeaponHealthBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus healthBonus = mock(StatBonus.class);
        when(stats.getWeaponHealthBonus()).thenReturn(healthBonus);

        SkillEffect.byName("WEAPON_DURABILITY_PERCENT").apply(stats, "mod_id", 20f);

        verify(healthBonus).modifyPercent("mod_id", 20f);
    }

    @Test
    void engineDurabilityModifiesTheEngineHealthBonusStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus healthBonus = mock(StatBonus.class);
        when(stats.getEngineHealthBonus()).thenReturn(healthBonus);

        DefenseSkillEffect.ENGINE_DURABILITY_PERCENT.apply(stats, "mod_id", 15f);

        verify(healthBonus).modifyPercent("mod_id", 15f);
    }

    @Test
    void topSpeedModifiesTheMaxSpeedStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat maxSpeed = mock(MutableStat.class);
        when(stats.getMaxSpeed()).thenReturn(maxSpeed);

        MovementSkillEffect.TOP_SPEED_PERCENT.apply(stats, "mod_id", 15f);

        verify(maxSpeed).modifyPercent("mod_id", 15f);
    }

    @Test
    void peakCrDurationModifiesThePeakCRDurationStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus peakCrDuration = mock(StatBonus.class);
        when(stats.getPeakCRDuration()).thenReturn(peakCrDuration);

        MiscSkillEffect.PEAK_CR_DURATION_PERCENT.apply(stats, "mod_id", 20f);

        verify(peakCrDuration).modifyPercent("mod_id", 20f);
    }

    @Test
    void weaponRangeFalloffModifiesTheWeaponRangeMultPastThresholdStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat rangeMultPastThreshold = mock(MutableStat.class);
        when(stats.getWeaponRangeMultPastThreshold()).thenReturn(rangeMultPastThreshold);

        SkillEffect.byName("WEAPON_RANGE_FALLOFF_PERCENT").apply(stats, "mod_id", -15f);

        verify(rangeMultPastThreshold).modifyPercent("mod_id", -15f);
    }

    @Test
    void repairTimeModifiesBothWeaponAndEngineRepairTimeStats() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat weaponRepairTime = mock(MutableStat.class);
        MutableStat engineRepairTime = mock(MutableStat.class);
        when(stats.getCombatWeaponRepairTimeMult()).thenReturn(weaponRepairTime);
        when(stats.getCombatEngineRepairTimeMult()).thenReturn(engineRepairTime);

        DefenseSkillEffect.REPAIR_TIME_PERCENT.apply(stats, "mod_id", -20f);

        verify(weaponRepairTime).modifyPercent("mod_id", -20f);
        verify(engineRepairTime).modifyPercent("mod_id", -20f);
    }

    @Test
    void missileGuidanceModifiesTheMissileGuidanceStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat missileGuidance = mock(MutableStat.class);
        when(stats.getMissileGuidance()).thenReturn(missileGuidance);

        SkillEffect.byName("MISSILE_WEAPON_GUIDANCE_FLAT").apply(stats, "mod_id", 1f);

        verify(missileGuidance).modifyFlat("mod_id", 1f);
    }

    @Test
    void crRecoveryRateModifiesTheBaseCRRecoveryRateStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat crRecoveryRate = mock(MutableStat.class);
        when(stats.getBaseCRRecoveryRatePercentPerDay()).thenReturn(crRecoveryRate);

        LogisticsSkillEffect.CR_RECOVERY_RATE_PERCENT.apply(stats, "mod_id", 15f);

        verify(crRecoveryRate).modifyPercent("mod_id", 15f);
    }

    @Test
    void crewLossModifiesTheCrewLossMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat crewLossMult = mock(MutableStat.class);
        when(stats.getCrewLossMult()).thenReturn(crewLossMult);

        LogisticsSkillEffect.CREW_LOSS_PERCENT.apply(stats, "mod_id", -15f);

        verify(crewLossMult).modifyPercent("mod_id", -15f);
    }

    @Test
    void energyDamageTakenModifiesTheEnergyDamageTakenMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat damageTakenMult = mock(MutableStat.class);
        MutableStat shieldDamageTakenMult = mock(MutableStat.class);
        when(stats.getEnergyDamageTakenMult()).thenReturn(damageTakenMult);
        when(stats.getEnergyShieldDamageTakenMult()).thenReturn(shieldDamageTakenMult);

        DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT.apply(stats, "mod_id", -10f);

        verify(damageTakenMult).modifyPercent("mod_id", -10f);
        verify(shieldDamageTakenMult).modifyPercent("mod_id", -10f);
    }

    @Test
    void describeUsesIncreasesForAPositiveBidirectionalMagnitude() {
        assertEquals("20% increased peak combat readiness duration.", MiscSkillEffect.PEAK_CR_DURATION_PERCENT.description(20f).plain());
    }

    @Test
    void describeUsesDecreasesForANegativeBidirectionalMagnitude() {
        assertEquals("20% reduced peak combat readiness duration.", MiscSkillEffect.PEAK_CR_DURATION_PERCENT.description(-20f).plain());
    }

    @Test
    void fighterBaysFlatModifiesTheNumFighterBaysStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat numFighterBays = mock(MutableStat.class);
        when(stats.getNumFighterBays()).thenReturn(numFighterBays);

        FighterSkillEffect.FIGHTER_BAYS_FLAT.apply(stats, "mod_id", 1f);

        verify(numFighterBays).modifyFlat("mod_id", 1f);
    }

    @Test
    void fighterBaysFlatDescribesTheBayCountOnly() {
        assertEquals("Increases number of fighter bays by 1.", FighterSkillEffect.FIGHTER_BAYS_FLAT.description(1f).plain());
    }

    @Test
    void fighterBaysFlatHasADeallocationWarning() {
        assertEquals("Unallocating it returns any fighter wing in the bay it adds to your cargo.",
                FighterSkillEffect.FIGHTER_BAYS_FLAT.deallocationWarning(1f).plain());
    }

    @Test
    void mostEffectsHaveNoDeallocationWarning() {
        assertNull(DefenseSkillEffect.HULL_PERCENT.deallocationWarning(10f));
    }

    @Test
    void fighterWeaponDamageDoesNotTouchTheCarrierStatsDirectly() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);

        FighterSkillEffect.FIGHTER_WEAPON_DAMAGE_PERCENT.apply(stats, "mod_id", 15f);

        verifyNoInteractions(stats);
    }

    @Test
    void fighterWeaponDamageModifiesTheFighterSOwnWeaponDamageStats() {
        ShipAPI fighter = mock(ShipAPI.class);
        ShipAPI parentShip = mock(ShipAPI.class);
        MutableShipStatsAPI fighterStats = mock(MutableShipStatsAPI.class);
        when(fighter.getMutableStats()).thenReturn(fighterStats);
        MutableStat ballistic = mock(MutableStat.class);
        MutableStat missile = mock(MutableStat.class);
        MutableStat energy = mock(MutableStat.class);
        MutableStat beam = mock(MutableStat.class);
        when(fighterStats.getBallisticWeaponDamageMult()).thenReturn(ballistic);
        when(fighterStats.getMissileWeaponDamageMult()).thenReturn(missile);
        when(fighterStats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(fighterStats.getBeamWeaponDamageMult()).thenReturn(beam);

        FighterSkillEffect.FIGHTER_WEAPON_DAMAGE_PERCENT.applyToFighterSpawnedByShip(fighter, parentShip, "mod_id", 15f);

        verify(ballistic).modifyPercent("mod_id", 15f);
        verify(missile).modifyPercent("mod_id", 15f);
        verify(energy).modifyPercent("mod_id", 15f);
        verifyNoInteractions(beam);
    }

    @Test
    void fighterTopSpeedDoesNotTouchTheCarrierStatsDirectly() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);

        FighterSkillEffect.FIGHTER_TOP_SPEED_PERCENT.apply(stats, "mod_id", 15f);

        verifyNoInteractions(stats);
    }

    @Test
    void fighterTopSpeedModifiesTheFighterSOwnMaxSpeedStat() {
        ShipAPI fighter = mock(ShipAPI.class);
        ShipAPI parentShip = mock(ShipAPI.class);
        MutableShipStatsAPI fighterStats = mock(MutableShipStatsAPI.class);
        when(fighter.getMutableStats()).thenReturn(fighterStats);
        MutableStat maxSpeed = mock(MutableStat.class);
        when(fighterStats.getMaxSpeed()).thenReturn(maxSpeed);

        FighterSkillEffect.FIGHTER_TOP_SPEED_PERCENT.applyToFighterSpawnedByShip(fighter, parentShip, "mod_id", 15f);

        verify(maxSpeed).modifyPercent("mod_id", 15f);
    }

    @Test
    void shieldDamageTakenIsLowerIsBetterForEveryFighterRoleLikeTheAllFighterVersion() {
        assertTrue(FighterSkillEffect.FIGHTER_SHIELD_DAMAGE_TAKEN_PERCENT.lowerIsBetter());
        assertTrue(FighterSkillEffect.FIGHTER_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT.lowerIsBetter());
        assertTrue(FighterSkillEffect.INTERCEPTOR_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT.lowerIsBetter());
        assertTrue(FighterSkillEffect.BOMBER_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT.lowerIsBetter());
        assertTrue(FighterSkillEffect.SUPPORT_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT.lowerIsBetter());
        assertFalse(FighterSkillEffect.BOMBER_ROLE_ARMOR_PERCENT.lowerIsBetter());
        assertFalse(FighterSkillEffect.FIGHTER_TOP_SPEED_PERCENT.lowerIsBetter());
    }

    @Test
    void removeAllFighterBaysTakesAwayOnlyTheHullsOwnBaysLikeVanilla() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat numFighterBays = new MutableStat(2f);
        numFighterBays.modifyFlat("other", 1f);
        when(stats.getNumFighterBays()).thenReturn(numFighterBays);

        FighterSkillEffect.REMOVE_ALL_FIGHTER_BAYS.apply(stats, "mod_id", 0f);

        assertEquals(1f, numFighterBays.getModifiedValue(), 1e-4f);
    }

    @Test
    void convertedFighterBayCanOnlyBeAllocatedWhenEveryWingIsBuiltIn() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType front = com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT;
        ShipFacts builtInWingsOnly = new ShipFacts(ShipAPI.HullSize.CRUISER, front, false, 1000f, false, hullModId -> false, true);

        assertNull(FighterSkillEffect.REMOVE_ALL_FIGHTER_BAYS.blockAllocationReason(builtInWingsOnly, front));
        assertNotNull(FighterSkillEffect.REMOVE_ALL_FIGHTER_BAYS.blockAllocationReason(ANY_SHIP, front));
    }

    @Test
    void convertedHangarAddsTheExtraBayVastHangarGrants() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        StatBonus convertedHangarMod = new StatBonus();
        convertedHangarMod.modifyFlat("vast_hangar", 1f);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(com.fs.starfarer.api.impl.campaign.ids.Stats.CONVERTED_HANGAR_MOD)).thenReturn(convertedHangarMod);
        MutableStat numFighterBays = new MutableStat(0f);
        when(stats.getNumFighterBays()).thenReturn(numFighterBays);

        FighterSkillEffect.CONVERTED_HANGAR_FIGHTER_BAYS_FLAT.apply(stats, "mod_id", 1f);

        assertEquals(2f, numFighterBays.getModifiedValue(), 1e-4f);
    }

    @Test
    void cargoCapacityPerFighterBayScalesWithTheShipSOwnBayCount() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat numFighterBays = mock(MutableStat.class);
        when(stats.getNumFighterBays()).thenReturn(numFighterBays);
        when(numFighterBays.getBaseValue()).thenReturn(3f);
        StatBonus cargoMod = mock(StatBonus.class);
        when(stats.getCargoMod()).thenReturn(cargoMod);

        LogisticsSkillEffect.CARGO_CAPACITY_PER_FIGHTER_BAY.apply(stats, "mod_id", 50f);

        verify(cargoMod).modifyFlat("mod_id", 150f);
    }

    @Test
    void minCrewPercentPerFighterBayScalesWithBayCount() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat numFighterBays = mock(MutableStat.class);
        when(stats.getNumFighterBays()).thenReturn(numFighterBays);
        when(numFighterBays.getBaseValue()).thenReturn(2f);
        StatBonus minCrewMod = mock(StatBonus.class);
        when(stats.getMinCrewMod()).thenReturn(minCrewMod);

        LogisticsSkillEffect.MIN_CREW_PERCENT_PER_FIGHTER_BAY.apply(stats, "mod_id", -20f);

        verify(minCrewMod).modifyPercent("mod_id", -40f);
    }

    @Test
    void minCrewPercentPerFighterBayIsCappedAtNegativeEighty() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat numFighterBays = mock(MutableStat.class);
        when(stats.getNumFighterBays()).thenReturn(numFighterBays);
        when(numFighterBays.getBaseValue()).thenReturn(6f);
        StatBonus minCrewMod = mock(StatBonus.class);
        when(stats.getMinCrewMod()).thenReturn(minCrewMod);

        LogisticsSkillEffect.MIN_CREW_PERCENT_PER_FIGHTER_BAY.apply(stats, "mod_id", -20f);

        verify(minCrewMod).modifyPercent("mod_id", -80f);
    }

    @Test
    void removeShieldSetsShieldTypeToNoneAfterShipCreation() {
        ShipAPI ship = mock(ShipAPI.class);

        ShieldSkillEffect.REMOVE_SHIELD.applyAfterShipCreation(ship, "mod_id", 0f);

        verify(ship).setShield(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE, 0f, 1f, 1f);
    }

    @Test
    void createFrontShieldIfNoneInstallsAShieldWhenTheShipHasNone() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getShield()).thenReturn(null);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getShieldType()).thenReturn(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE);
        when(ship.getHullSpec()).thenReturn(hullSpec);

        ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE.applyAfterShipCreation(ship, "mod_id", 0f);

        verify(ship).setShield(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, 0.5f, 1.2f, 90f);
    }

    @Test
    void createFrontShieldIfNoneDoesNothingWhenTheShipAlreadyHasAShield() {
        ShipAPI ship = mock(ShipAPI.class);
        com.fs.starfarer.api.combat.ShieldAPI existingShield = mock(com.fs.starfarer.api.combat.ShieldAPI.class);
        when(ship.getShield()).thenReturn(existingShield);

        ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE.applyAfterShipCreation(ship, "mod_id", 0f);

        verify(ship, never()).setShield(any(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void convertShieldToFrontChangesAnExistingShieldSType() {
        ShipAPI ship = mock(ShipAPI.class);
        com.fs.starfarer.api.combat.ShieldAPI shield = mock(com.fs.starfarer.api.combat.ShieldAPI.class);
        when(ship.getShield()).thenReturn(shield);

        ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT.applyAfterShipCreation(ship, "mod_id", 0f);

        verify(shield).setType(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT);
    }

    @Test
    void convertShieldToFrontDoesNothingWhenTheShipHasNoShield() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getShield()).thenReturn(null);

        assertDoesNotThrow(() -> ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT.applyAfterShipCreation(ship, "mod_id", 0f));
    }

    @Test
    void convertShieldToOmniChangesAnExistingShieldSType() {
        ShipAPI ship = mock(ShipAPI.class);
        com.fs.starfarer.api.combat.ShieldAPI shield = mock(com.fs.starfarer.api.combat.ShieldAPI.class);
        when(ship.getShield()).thenReturn(shield);

        ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI.applyAfterShipCreation(ship, "mod_id", 0f);

        verify(shield).setType(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI);
    }

    @Test
    void resolveDisplayShieldTypeConvertsAnExistingShieldToFront() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI, List.of(ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, result);
    }

    @Test
    void resolveDisplayShieldTypeConvertsAnExistingShieldToOmni() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, List.of(ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI, result);
    }

    @Test
    void resolveDisplayShieldTypeDoesNothingWhenTheHullHasNoShieldToConvert() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE, List.of(ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE, result);
    }

    @Test
    void resolveDisplayShieldTypeRemovesTheShield() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, List.of(ShieldSkillEffect.REMOVE_SHIELD));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE, result);
    }

    @Test
    void resolveDisplayShieldTypeCreatesAFrontShieldWhenTheHullHasNone() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE, List.of(ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, result);
    }

    @Test
    void resolveDisplayShieldTypeDoesNotOverrideAnExistingShieldWithTheMakeshiftOne() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI, List.of(ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI, result);
    }

    @Test
    void resolveDisplayShieldTypeAppliesEffectsInAllocationOrder() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE,
                List.of(ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE, ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI, result);
    }

    @Test
    void resolveDisplayShieldTypeIgnoresUnrelatedEffects() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType result = ShieldSkillEffect.resolveDisplayShieldType(
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, List.of(ShieldSkillEffect.SHIELD_ARC_PERCENT));

        assertEquals(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, result);
    }

    private static final ShipFacts ANY_SHIP = new ShipFacts(ShipAPI.HullSize.CRUISER,
            com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT, false, 1000f, hullModId -> false);

    private static String shieldBlock(SkillEffect effect, com.fs.starfarer.api.combat.ShieldAPI.ShieldType currentShieldType) {
        return effect.blockAllocationReason(ANY_SHIP, currentShieldType);
    }

    @Test
    void shieldConversionsAreBlockedOnlyWhenTheShipAlreadyHasThatShield() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType front = com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT;
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType omni = com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI;

        assertEquals("Ship already has front shields.", shieldBlock(ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT, front));
        assertNull(shieldBlock(ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT, omni));
        assertEquals("Ship already has omni-directional shields.", shieldBlock(ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI, omni));
        assertNull(shieldBlock(ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI, front));
        assertNull(shieldBlock(ShieldSkillEffect.SHIELD_ARC_PERCENT, front));
    }

    @Test
    void removeShieldNeedsAShieldAndThePhaseCloakIsNotOne() {
        assertEquals("Ship has no shields.",
                shieldBlock(ShieldSkillEffect.REMOVE_SHIELD, com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE));
        assertEquals("Ship has no shields.",
                shieldBlock(ShieldSkillEffect.REMOVE_SHIELD, com.fs.starfarer.api.combat.ShieldAPI.ShieldType.PHASE));
        assertNull(shieldBlock(ShieldSkillEffect.REMOVE_SHIELD, com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT));
        assertNull(shieldBlock(ShieldSkillEffect.REMOVE_SHIELD, com.fs.starfarer.api.combat.ShieldAPI.ShieldType.OMNI));
    }

    @Test
    void theRecoveryBonusIsWithheldFromNpcShipsWhileOtherEffectsApply() {
        assertFalse(DefenseSkillEffect.SHIP_RECOVERY_CHANCE_BONUS.appliesToNpcShips());
        assertTrue(DefenseSkillEffect.HULL_PERCENT.appliesToNpcShips());
        assertTrue(DefenseSkillEffect.BREAK_PROBABILITY_PERCENT.appliesToNpcShips());
    }

    @Test
    void minCrewPerFighterBayScalesWithTheShipSOwnBayCount() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat numFighterBays = mock(MutableStat.class);
        when(stats.getNumFighterBays()).thenReturn(numFighterBays);
        when(numFighterBays.getBaseValue()).thenReturn(3f);
        StatBonus minCrewMod = mock(StatBonus.class);
        when(stats.getMinCrewMod()).thenReturn(minCrewMod);

        LogisticsSkillEffect.MIN_CREW_PER_FIGHTER_BAY.apply(stats, "mod_id", 20f);

        verify(minCrewMod).modifyFlat("mod_id", 60f);
    }

    @Test
    void fighterReplacementDecayMultModifiesTheDecreaseMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        MutableStat decreaseMult = mock(MutableStat.class);
        when(dynamic.getStat("replacement_rate_decrease_mult")).thenReturn(decreaseMult);

        FighterSkillEffect.FIGHTER_REPLACEMENT_DECAY_PERCENT.apply(stats, "mod_id", -15f);

        verify(decreaseMult).modifyPercent("mod_id", -15f);
    }

    @Test
    void fighterReplacementRecoveryMultModifiesTheIncreaseMultStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        MutableStat increaseMult = mock(MutableStat.class);
        when(dynamic.getStat("replacement_rate_increase_mult")).thenReturn(increaseMult);

        FighterSkillEffect.FIGHTER_REPLACEMENT_RECOVERY_PERCENT.apply(stats, "mod_id", 25f);

        verify(increaseMult).modifyPercent("mod_id", 25f);
    }

    @Test
    void fighterPdDamageBonusDoesNotTouchTheCarrierStatsDirectly() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);

        FighterSkillEffect.FIGHTER_PD_DAMAGE_BONUS_PERCENT.apply(stats, "mod_id", 50f);

        verifyNoInteractions(stats);
    }

    @Test
    void fighterPdDamageBonusModifiesTheFighterSOwnDamageToFightersAndMissiles() {
        ShipAPI fighter = mock(ShipAPI.class);
        ShipAPI parentShip = mock(ShipAPI.class);
        MutableShipStatsAPI fighterStats = mock(MutableShipStatsAPI.class);
        when(fighter.getMutableStats()).thenReturn(fighterStats);
        MutableStat damageToFighters = mock(MutableStat.class);
        MutableStat damageToMissiles = mock(MutableStat.class);
        when(fighterStats.getDamageToFighters()).thenReturn(damageToFighters);
        when(fighterStats.getDamageToMissiles()).thenReturn(damageToMissiles);

        FighterSkillEffect.FIGHTER_PD_DAMAGE_BONUS_PERCENT.applyToFighterSpawnedByShip(fighter, parentShip, "mod_id", 50f);

        verify(damageToFighters).modifyPercent("mod_id", 50f);
        verify(damageToMissiles).modifyPercent("mod_id", 50f);
    }

    @Test
    void beamDamageHardFluxPercentModifiesTheDynamicStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        StatBonus hardFluxPercent = mock(StatBonus.class);
        when(dynamic.getMod("exiledSector_beamDamageHardFluxPercent")).thenReturn(hardFluxPercent);

        ShieldSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT.apply(stats, "mod_id", 50f);

        verify(hardFluxPercent).modifyFlat("mod_id", 50f);
    }

    @Test
    void beamDamageHardFluxPercentAddsAListenerOnce() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.hasListenerOfClass(any())).thenReturn(false);

        ShieldSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT.applyAfterShipCreation(ship, "mod_id", 50f);

        verify(ship).addListener(any(DamageDealtModifier.class));
    }

    @Test
    void beamDamageHardFluxPercentDoesNotDuplicateTheListener() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.hasListenerOfClass(any())).thenReturn(true);

        ShieldSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT.applyAfterShipCreation(ship, "mod_id", 50f);

        verify(ship, never()).addListener(any());
    }

    @Test
    void phaseAnchorEmergencyDiveModifiesTheDynamicCrPenaltyStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        StatBonus crPenalty = mock(StatBonus.class);
        when(dynamic.getMod("exiledSector_phaseAnchorCrPenaltyPercent")).thenReturn(crPenalty);

        PhaseSkillEffect.PHASE_ANCHOR_EMERGENCY_DIVE.apply(stats, "mod_id", 100f);

        verify(crPenalty).modifyFlat("mod_id", 100f);
    }

    @Test
    void phaseAnchorEmergencyDiveAddsAListenerOnce() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.hasListenerOfClass(any())).thenReturn(false);

        PhaseSkillEffect.PHASE_ANCHOR_EMERGENCY_DIVE.applyAfterShipCreation(ship, "mod_id", 100f);

        verify(ship).addListener(any(HullDamageAboutToBeTakenListener.class));
    }

    @Test
    void phaseAnchorEmergencyDiveDoesNotDuplicateTheListener() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.hasListenerOfClass(any())).thenReturn(true);

        PhaseSkillEffect.PHASE_ANCHOR_EMERGENCY_DIVE.applyAfterShipCreation(ship, "mod_id", 100f);

        verify(ship, never()).addListener(any());
    }

    private static ShipAPI phaseShip() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getPhaseCloak()).thenReturn(mock(ShipSystemAPI.class));
        return ship;
    }

    private Object capturePhaseAnchorDiveListener(ShipAPI ship, float magnitude) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_phaseAnchorCrPenaltyPercent", 0f)).thenReturn(magnitude);
        when(ship.hasListenerOfClass(any())).thenReturn(false);
        PhaseSkillEffect.PHASE_ANCHOR_EMERGENCY_DIVE.applyAfterShipCreation(ship, "mod_id", magnitude);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        return captor.getValue();
    }

    @Test
    void phaseAnchorDiveIgnoresNonLethalDamage() {
        ShipAPI ship = phaseShip();
        when(ship.getHitpoints()).thenReturn(100f);
        HullDamageAboutToBeTakenListener listener = (HullDamageAboutToBeTakenListener) capturePhaseAnchorDiveListener(ship, 100f);

        boolean saved = listener.notifyAboutToTakeHullDamage(new Object(), ship, mock(Vector2f.class), 50f);

        assertFalse(saved);
        verify(ship, never()).setHitpoints(anyFloat());
    }

    @Test
    void phaseAnchorDiveSavesTheShipOnLethalDamageAndAppliesTheCrPenalty() {
        ShipAPI ship = phaseShip();
        when(ship.getHitpoints()).thenReturn(100f);
        when(ship.getCurrentCR()).thenReturn(20f);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        RepairTrackerAPI repairTracker = mock(RepairTrackerAPI.class);
        when(member.getDeployCost()).thenReturn(10f);
        when(member.getRepairTracker()).thenReturn(repairTracker);
        when(ship.getFleetMember()).thenReturn(member);
        HullDamageAboutToBeTakenListener listener = (HullDamageAboutToBeTakenListener) capturePhaseAnchorDiveListener(ship, 100f);

        Map<String, Object> customData = new HashMap<>();
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            when(engine.getCustomData()).thenReturn(customData);

            boolean saved = listener.notifyAboutToTakeHullDamage(new Object(), ship, mock(Vector2f.class), 150f);

            assertTrue(saved);
        }
        verify(ship).setHitpoints(1f);
        verify(repairTracker).applyCREvent(-10f, "Emergency phase dive");
        assertEquals(Boolean.TRUE, customData.get("phaseAnchor_canDive"));
    }

    @Test
    void phaseAnchorDiveTreatsAMissingFleetMemberAsZeroDeployCost() {
        ShipAPI ship = phaseShip();
        when(ship.getHitpoints()).thenReturn(100f);
        when(ship.getCurrentCR()).thenReturn(0f);
        when(ship.getFleetMember()).thenReturn(null);
        HullDamageAboutToBeTakenListener listener = (HullDamageAboutToBeTakenListener) capturePhaseAnchorDiveListener(ship, 100f);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            when(engine.getCustomData()).thenReturn(new HashMap<>());

            boolean saved = listener.notifyAboutToTakeHullDamage(new Object(), ship, mock(Vector2f.class), 150f);

            assertTrue(saved);
        }
        verify(ship).setHitpoints(1f);
    }

    @Test
    void phaseAnchorDiveIsBlockedWhenAnotherShipAlreadyDoveThisBattle() {
        ShipAPI ship = phaseShip();
        when(ship.getHitpoints()).thenReturn(100f);
        HullDamageAboutToBeTakenListener listener = (HullDamageAboutToBeTakenListener) capturePhaseAnchorDiveListener(ship, 100f);

        Map<String, Object> customData = new HashMap<>();
        customData.put("phaseAnchor_canDive", Boolean.TRUE);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            when(engine.getCustomData()).thenReturn(customData);

            boolean saved = listener.notifyAboutToTakeHullDamage(new Object(), ship, mock(Vector2f.class), 150f);

            assertFalse(saved);
        }
        verify(ship, never()).setHitpoints(anyFloat());
    }

    @Test
    void phaseAnchorDiveIsBlockedByInsufficientCombatReadiness() {
        ShipAPI ship = phaseShip();
        when(ship.getHitpoints()).thenReturn(100f);
        when(ship.getCurrentCR()).thenReturn(5f);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getDeployCost()).thenReturn(10f);
        when(ship.getFleetMember()).thenReturn(member);
        HullDamageAboutToBeTakenListener listener = (HullDamageAboutToBeTakenListener) capturePhaseAnchorDiveListener(ship, 100f);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            when(engine.getCustomData()).thenReturn(new HashMap<>());

            boolean saved = listener.notifyAboutToTakeHullDamage(new Object(), ship, mock(Vector2f.class), 150f);

            assertFalse(saved);
        }
        verify(ship, never()).setHitpoints(anyFloat());
    }

    @Test
    void phaseAnchorDiveNeverSavesAShipWithoutAPhaseCloak() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getHitpoints()).thenReturn(100f);
        HullDamageAboutToBeTakenListener listener = (HullDamageAboutToBeTakenListener) capturePhaseAnchorDiveListener(ship, 0f);

        assertFalse(listener.notifyAboutToTakeHullDamage(new Object(), ship, mock(Vector2f.class), 150f));
        assertFalse(listener.notifyAboutToTakeHullDamage(new Object(), ship, mock(Vector2f.class), 150f));
        verify(ship, never()).setHitpoints(anyFloat());
    }

    @Test
    void phaseAnchorEmergencyDiveCanOnlyBeAllocatedOnPhaseHulls() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType phaseCloak = com.fs.starfarer.api.combat.ShieldAPI.ShieldType.PHASE;
        ShipFacts phase = new ShipFacts(ShipAPI.HullSize.CRUISER, phaseCloak, true, 500f, hullModId -> false);

        assertNull(PhaseSkillEffect.PHASE_ANCHOR_EMERGENCY_DIVE.blockAllocationReason(phase, phaseCloak));
        assertNotNull(PhaseSkillEffect.PHASE_ANCHOR_EMERGENCY_DIVE.blockAllocationReason(ANY_SHIP,
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.FRONT));
    }

    @Test
    void phaseAnchorDiveAdvanceDoesNothingWhenNotDiving() {
        ShipAPI ship = mock(ShipAPI.class);
        AdvanceableListener listener = (AdvanceableListener) capturePhaseAnchorDiveListener(ship, 100f);

        listener.advance(0.1f);

        verify(ship, never()).setRetreating(true, false);
    }

    @Test
    void phaseAnchorDiveMakesTheShipHullInvulnerableAndRetreatingThenRemovesItFromTheBattlefield() {
        ShipAPI ship = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(ship.getHitpoints()).thenReturn(100f);
        when(ship.getHullSize()).thenReturn(ShipAPI.HullSize.CRUISER);
        Vector2f location = new Vector2f(10f, 10f);
        when(ship.getLocation()).thenReturn(location);
        when(ship.getFluxTracker().showFloaty()).thenReturn(true);
        ShipSystemAPI phaseCloak = mock(ShipSystemAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(phaseCloak.getSpecAPI().getEffectColor2()).thenReturn(java.awt.Color.CYAN);
        when(phaseCloak.getChargeUpDur()).thenReturn(1f);
        when(ship.getPhaseCloak()).thenReturn(phaseCloak);
        Object listener = capturePhaseAnchorDiveListener(ship, 0f);
        MutableStat hullDamageTaken = mock(MutableStat.class);
        when(ship.getMutableStats().getTimeMult()).thenReturn(new MutableStat(1f));
        when(ship.getMutableStats().getHullDamageTakenMult()).thenReturn(hullDamageTaken);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            SoundPlayerAPI sound = mock(SoundPlayerAPI.class);
            SettingsAPI settings = mock(SettingsAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            globalMock.when(Global::getSettings).thenReturn(settings);
            globalMock.when(Global::getSoundPlayer).thenReturn(sound);
            when(engine.getCustomData()).thenReturn(new HashMap<>());
            ((HullDamageAboutToBeTakenListener) listener).notifyAboutToTakeHullDamage(new Object(), ship, location, 150f);
            AdvanceableListener dive = (AdvanceableListener) listener;

            dive.advance(0.5f);
            verify(hullDamageTaken).modifyMult("mod_id", 0f);
            verify(ship).setRetreating(true, false);
            verify(ship).blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            verify(phaseCloak).forceState(ShipSystemAPI.SystemState.IN, 0.5f);
            verify(engine).addFloatingTextAlways(eq(location), eq("Emergency dive!"), anyFloat(), any(), eq(ship),
                    anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
            assertEquals(new Vector2f(10f, 10f), location);

            dive.advance(0.6f);
            assertEquals(new Vector2f(10f, 10f), location);
            dive.advance(0.5f);

            verify(sound).playSound(eq("phase_anchor_vanish"), eq(1f), eq(1f), eq(location), any());
            verify(engine).addFloatingTextAlways(any(), anyString(), anyFloat(), any(), any(),
                    anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
            assertEquals(new Vector2f(0f, -1000000f), location);
        }
    }

    @Test
    void beamSplitTargetsFlatModifiesTheDynamicStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        StatBonus splitTargets = mock(StatBonus.class);
        when(dynamic.getMod("exiledSector_beamSplitTargets")).thenReturn(splitTargets);

        CombatSkillEffect.BEAM_WEAPON_SPLIT_TARGETS_FLAT.apply(stats, "mod_id", 1f);

        verify(splitTargets).modifyFlat("mod_id", 1f);
    }

    @Test
    void beamSplitTargetsFlatDescribesTheSplitCount() {
        assertEquals("Beam weapon hits split their damage evenly across the target and up to 1 additional nearby enemy ship. Fighters are never chosen. "
                        + "The target acquisition range is half the beam weapon's range. "
                        + "Split beams also carry the weapon's special beam effects.",
                CombatSkillEffect.BEAM_WEAPON_SPLIT_TARGETS_FLAT.description(1f).plain());
        assertEquals("Beam weapon hits split their damage evenly across the target and up to 3 additional nearby enemy ships. Fighters are never chosen. "
                        + "The target acquisition range is half the beam weapon's range. "
                        + "Split beams also carry the weapon's special beam effects.",
                CombatSkillEffect.BEAM_WEAPON_SPLIT_TARGETS_FLAT.description(3f).plain());
    }

    @Test
    void beamSplitTargetsFlatAddsAListenerOnce() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.hasListenerOfClass(any())).thenReturn(false);

        CombatSkillEffect.BEAM_WEAPON_SPLIT_TARGETS_FLAT.applyAfterShipCreation(ship, "mod_id", 1f);

        verify(ship).addListener(any(DamageDealtModifier.class));
    }

    @Test
    void beamSplitTargetsFlatDoesNotDuplicateTheListener() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.hasListenerOfClass(any())).thenReturn(true);

        CombatSkillEffect.BEAM_WEAPON_SPLIT_TARGETS_FLAT.applyAfterShipCreation(ship, "mod_id", 1f);

        verify(ship, never()).addListener(any());
    }

    private DamageDealtModifier captureBeamSplitListener(ShipAPI ship) {
        when(ship.hasListenerOfClass(any())).thenReturn(false);
        CombatSkillEffect.BEAM_WEAPON_SPLIT_TARGETS_FLAT.applyAfterShipCreation(ship, "mod_id", 1f);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        return (DamageDealtModifier) captor.getValue();
    }

    private ShipAPI mockBeamSplitEnemy(int owner, Vector2f location) {
        ShipAPI enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(owner);
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.isHulk()).thenReturn(false);
        when(enemy.getLocation()).thenReturn(location);
        when(enemy.getFluxTracker()).thenReturn(mock(FluxTrackerAPI.class));
        return enemy;
    }

    @Test
    void beamSplitListenerIgnoresNonBeamDamageSources() {
        ShipAPI ship = mock(ShipAPI.class);
        DamageDealtModifier listener = captureBeamSplitListener(ship);

        ShipAPI target = mock(ShipAPI.class);
        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);

        listener.modifyDamageDealt(new Object(), target, damage, new Vector2f(0f, 0f), false);

        verify(damage.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void beamSplitListenerIgnoresNonShipTargets() {
        ShipAPI ship = mock(ShipAPI.class);
        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        CombatEntityAPI target = mock(CombatEntityAPI.class);
        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);

        listener.modifyDamageDealt(beam, target, damage, new Vector2f(0f, 0f), false);

        verify(damage.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void beamSplitListenerDoesNothingWhenNoSplitTargetsGranted() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(0f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        ShipAPI target = mock(ShipAPI.class);
        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);

        listener.modifyDamageDealt(beam, target, damage, new Vector2f(0f, 0f), false);

        verify(damage.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    private WeaponAPI mockBeamSplitWeapon(BeamAPI beam, float range) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getRange()).thenReturn(range);
        when(beam.getWeapon()).thenReturn(weapon);
        mockBeamSplitDerivedStats(weapon, 100f, 0f);
        return weapon;
    }

    private void mockBeamSplitDerivedStats(WeaponAPI weapon, float dps, float empPerSecond) {
        WeaponAPI.DerivedWeaponStatsAPI derivedStats = mock(WeaponAPI.DerivedWeaponStatsAPI.class);
        when(derivedStats.getDps()).thenReturn(dps);
        when(derivedStats.getEmpPerSecond()).thenReturn(empPerSecond);
        when(weapon.getDerivedStats()).thenReturn(derivedStats);
    }

    @Test
    void beamSplitListenerSplitsDamageAcrossNearbyEnemies() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(2f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWidth()).thenReturn(10f);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        mockBeamSplitWeapon(beam, 2000f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);

        Vector2f point = new Vector2f(0f, 0f);
        ShipAPI enemy1 = mockBeamSplitEnemy(1, new Vector2f(100f, 0f));
        ShipAPI enemy2 = mockBeamSplitEnemy(1, new Vector2f(0f, 100f));

        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(90f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, enemy1, enemy2);
            fakeBeamMock.when(() -> MagicFakeBeam.getShipCollisionPoint(any(), any(), any(), anyFloat()))
                    .thenAnswer(invocation -> ((ShipAPI) invocation.getArgument(2)).getLocation());

            listener.modifyDamageDealt(beam, primaryTarget, damage, point, false);

            verify(damage.getModifier()).modifyMult("exiledSector_beamSplitShare", 1f / 3);
            verify(engine).applyDamage(eq(beam), eq(enemy1), any(Vector2f.class), eq(30f), eq(DamageType.ENERGY),
                    eq(0f), eq(false), eq(true), eq(ship), eq(false));
            verify(engine).applyDamage(eq(beam), eq(enemy2), any(Vector2f.class), eq(30f), eq(DamageType.ENERGY),
                    eq(0f), eq(false), eq(true), eq(ship), eq(false));
        }
    }

    @Test
    void beamSplitListenerKeepsHardFluxOnSplitHitsFromBeamsThatForceIt() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(1f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        mockBeamSplitWeapon(beam, 2000f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);
        ShipAPI enemy = mockBeamSplitEnemy(1, new Vector2f(100f, 0f));
        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(90f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);
        when(damage.isForceHardFlux()).thenReturn(true);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, enemy);
            fakeBeamMock.when(() -> MagicFakeBeam.getShipCollisionPoint(any(), any(), any(), anyFloat()))
                    .thenAnswer(invocation -> ((ShipAPI) invocation.getArgument(2)).getLocation());

            listener.modifyDamageDealt(beam, primaryTarget, damage, new Vector2f(0f, 0f), false);

            verify(engine).applyDamage(eq(beam), eq(enemy), any(Vector2f.class), eq(45f), eq(DamageType.ENERGY),
                    eq(0f), eq(false), eq(false), eq(ship), eq(false));
        }
    }

    private static void stubShipGrid(CombatEngineAPI engine, ShipAPI... ships) {
        CollisionGridAPI grid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(grid);
        when(grid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> List.<Object>of(ships).iterator());
    }

    @Test
    void beamSplitAppliesOnlyThisTicksShareOfPerSecondBeamDamageToSplitTargets() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(2f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        mockBeamSplitWeapon(beam, 2000f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);
        ShipAPI enemy1 = mockBeamSplitEnemy(1, new Vector2f(100f, 0f));
        ShipAPI enemy2 = mockBeamSplitEnemy(1, new Vector2f(0f, 100f));
        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.isDps()).thenReturn(true);
        when(damage.getDamage()).thenReturn(90f);
        when(damage.getDpsDuration()).thenReturn(0.1f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);

        String returnedModifierId;
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, enemy1, enemy2);
            fakeBeamMock.when(() -> MagicFakeBeam.getShipCollisionPoint(any(), any(), any(), anyFloat()))
                    .thenAnswer(invocation -> ((ShipAPI) invocation.getArgument(2)).getLocation());

            returnedModifierId = listener.modifyDamageDealt(beam, primaryTarget, damage, new Vector2f(0f, 0f), false);

            verify(engine).applyDamage(eq(beam), eq(enemy1), any(Vector2f.class), org.mockito.AdditionalMatchers.eq(3f, 0.0001f), eq(DamageType.ENERGY),
                    anyFloat(), eq(false), eq(true), eq(ship), eq(false));
        }
        verify(damage.getModifier()).modifyMult("exiledSector_beamSplitShare", 1f / 3);
        assertEquals("exiledSector_beamSplitShare", returnedModifierId);
    }

    @Test
    void beamSplitListenerFiresARealBeamFromADroneAtEachSplitTargetAndRemovesItWhenTheSplitEnds() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        when(ship.isAlive()).thenReturn(true);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(1f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        WeaponAPI weapon = mockBeamSplitWeapon(beam, 2000f);
        BeamWeaponSpecAPI spec = mock(BeamWeaponSpecAPI.class);
        when(weapon.getSpec()).thenReturn(spec);
        when(weapon.getSize()).thenReturn(WeaponAPI.WeaponSize.MEDIUM);
        when(spec.getWeaponId()).thenReturn("tachyonlance");

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);
        when(primaryTarget.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(0f, 0f));
        ShipAPI enemy = mockBeamSplitEnemy(1, new Vector2f(100f, 0f));
        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(90f);

        SettingsAPI settings = mock(SettingsAPI.class);
        com.fs.starfarer.api.combat.ShipHullSpecAPI hull = mock(com.fs.starfarer.api.combat.ShipHullSpecAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(settings.getHullSpec("exiledSector_split_beam_drone")).thenReturn(hull);
        when(settings.createEmptyVariant("exiledSector_split_beam_drone", hull)).thenReturn(variant);
        com.fs.starfarer.api.FactoryAPI factory = mock(com.fs.starfarer.api.FactoryAPI.class);
        com.fs.starfarer.api.characters.PersonAPI officerCopy = mock(com.fs.starfarer.api.characters.PersonAPI.class);
        when(factory.createPerson()).thenReturn(officerCopy);
        ShipAPI drone = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        when(drone.getAllWeapons()).thenReturn(List.of(droneWeapon));
        when(drone.getLocation()).thenReturn(new Vector2f());
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        stubShipGrid(engine, ship, primaryTarget, enemy);
        when(engine.createFXDrone(variant)).thenReturn(drone);

        String returnedModifierId;
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            globalMock.when(Global::getSettings).thenReturn(settings);
            globalMock.when(Global::getFactory).thenReturn(factory);

            when(shipStats.getTimeMult()).thenReturn(new MutableStat(1f));
            when(ship.getVariant()).thenReturn(mock(ShipVariantAPI.class));
            returnedModifierId = listener.modifyDamageDealt(beam, primaryTarget, damage, new Vector2f(0f, 0f), true);
            ArgumentCaptor<Object> droneListeners = ArgumentCaptor.forClass(Object.class);
            verify(drone, Mockito.atLeastOnce()).addListener(droneListeners.capture());
            AdvanceableListener split = droneListeners.getAllValues().stream().filter(AdvanceableListener.class::isInstance)
                    .map(AdvanceableListener.class::cast).findFirst().orElseThrow();
            split.advance(0.016f);

            verify(variant).addWeapon("WS MEDIUM", "tachyonlance");
            verify(engine).addEntity(drone);
            verify(drone).setCaptain(officerCopy);
            verify(droneWeapon).setForceFireOneFrame(true);
            verify(engine, never()).applyDamage(any(), any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());

            split.advance(1f);
            verify(engine).removeEntity(drone);
        }
        verify(damage.getModifier()).modifyMult("exiledSector_beamSplitShare", 1f / 2);
        assertEquals("exiledSector_beamSplitShare", returnedModifierId);
    }

    @Test
    void beamSplitListenerDoesNotReSplitItsOwnSyntheticHits() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(2f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWidth()).thenReturn(10f);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        mockBeamSplitWeapon(beam, 2000f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);

        Vector2f point = new Vector2f(0f, 0f);
        ShipAPI enemy1 = mockBeamSplitEnemy(1, new Vector2f(100f, 0f));
        ShipAPI enemy2 = mockBeamSplitEnemy(1, new Vector2f(0f, 100f));

        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(90f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, enemy1, enemy2);
            fakeBeamMock.when(() -> MagicFakeBeam.getShipCollisionPoint(any(), any(), any(), anyFloat()))
                    .thenAnswer(invocation -> ((ShipAPI) invocation.getArgument(2)).getLocation());
            // Simulate the real engine re-invoking every DamageDealtModifier (including this
            // very listener) when our own applyDamage(beam, ...) calls go through.
            Mockito.doAnswer(invocation -> {
                DamageAPI splitDamage = mock(DamageAPI.class);
                when(splitDamage.getDamage()).thenReturn(invocation.getArgument(3));
                when(splitDamage.getType()).thenReturn(invocation.getArgument(4));
                listener.modifyDamageDealt(invocation.getArgument(0), invocation.getArgument(1), splitDamage,
                        invocation.getArgument(2), false);
                return null;
            }).when(engine).applyDamage(any(), any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());

            listener.modifyDamageDealt(beam, primaryTarget, damage, point, false);

            verify(engine, times(2)).applyDamage(eq(beam), any(), any(), anyFloat(), any(),
                    anyFloat(), anyBoolean(), anyBoolean(), eq(ship), anyBoolean());
        }
    }

    @Test
    void beamSplitListenerOnlySplitsAcrossEnemiesActuallyInRange() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(2f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWidth()).thenReturn(10f);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        mockBeamSplitWeapon(beam, 2000f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);

        Vector2f point = new Vector2f(0f, 0f);
        ShipAPI enemyInRange = mockBeamSplitEnemy(1, new Vector2f(100f, 0f));
        ShipAPI enemyOutOfRange = mockBeamSplitEnemy(1, new Vector2f(5000f, 0f));

        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(90f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, enemyInRange, enemyOutOfRange);
            fakeBeamMock.when(() -> MagicFakeBeam.getShipCollisionPoint(any(), any(), any(), anyFloat()))
                    .thenAnswer(invocation -> ((ShipAPI) invocation.getArgument(2)).getLocation());

            listener.modifyDamageDealt(beam, primaryTarget, damage, point, false);

            verify(damage.getModifier()).modifyMult("exiledSector_beamSplitShare", 1f / 2);
            verify(engine).applyDamage(eq(beam), eq(enemyInRange), any(Vector2f.class), eq(45f), eq(DamageType.ENERGY),
                    eq(0f), eq(false), eq(true), eq(ship), eq(false));
            verify(engine, never()).applyDamage(eq(beam), eq(enemyOutOfRange), any(Vector2f.class), anyFloat(), any(),
                    anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());
        }
    }

    @Test
    void beamSplitRadiusIsHalfTheBeamsModifiedRange() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(1f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWidth()).thenReturn(10f);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        // Range 1000 -> split radius 500: an enemy at 600 should be excluded, but included once range doubles.
        mockBeamSplitWeapon(beam, 1000f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);

        Vector2f point = new Vector2f(0f, 0f);
        ShipAPI enemyJustOutOfHalfRange = mockBeamSplitEnemy(1, new Vector2f(600f, 0f));

        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(90f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, enemyJustOutOfHalfRange);
            fakeBeamMock.when(() -> MagicFakeBeam.getShipCollisionPoint(any(), any(), any(), anyFloat()))
                    .thenAnswer(invocation -> ((ShipAPI) invocation.getArgument(2)).getLocation());

            listener.modifyDamageDealt(beam, primaryTarget, damage, point, false);
            verify(damage.getModifier(), never()).modifyMult(anyString(), anyFloat());

            when(beam.getWeapon().getRange()).thenReturn(1400f);
            listener.modifyDamageDealt(beam, primaryTarget, damage, point, false);
            verify(damage.getModifier()).modifyMult("exiledSector_beamSplitShare", 1f / 2);
        }
    }

    @Test
    void beamSplitListenerSplitsEmpProportionallyAcrossTargets() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(1f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWidth()).thenReturn(10f);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        WeaponAPI weapon = mockBeamSplitWeapon(beam, 2000f);
        // dps=100, emp/sec=20 -> a hit dealing 40 damage should carry 8 emp (40 * 20/100).
        mockBeamSplitDerivedStats(weapon, 100f, 20f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);

        Vector2f point = new Vector2f(0f, 0f);
        ShipAPI enemy = mockBeamSplitEnemy(1, new Vector2f(100f, 0f));

        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(80f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, enemy);
            fakeBeamMock.when(() -> MagicFakeBeam.getShipCollisionPoint(any(), any(), any(), anyFloat()))
                    .thenAnswer(invocation -> ((ShipAPI) invocation.getArgument(2)).getLocation());

            listener.modifyDamageDealt(beam, primaryTarget, damage, point, false);

            verify(damage.getModifier()).modifyMult("exiledSector_beamSplitShare", 1f / 2);
            verify(engine).applyDamage(eq(beam), eq(enemy), any(Vector2f.class), eq(40f), eq(DamageType.ENERGY),
                    eq(8f), eq(false), eq(true), eq(ship), eq(false));
        }
    }

    @Test
    void findNearbyEnemiesExcludesNeutralOwnedShipsEvenThoughTheirOwnerDiffers() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI shipStats = mock(MutableShipStatsAPI.class);
        com.fs.starfarer.api.util.DynamicStatsAPI dynamic = mock(com.fs.starfarer.api.util.DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(shipStats);
        when(shipStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_beamSplitTargets", 0f)).thenReturn(1f);

        DamageDealtModifier listener = captureBeamSplitListener(ship);

        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWidth()).thenReturn(10f);
        when(beam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(beam.getFringeColor()).thenReturn(java.awt.Color.RED);
        mockBeamSplitWeapon(beam, 2000f);

        ShipAPI primaryTarget = mock(ShipAPI.class);
        when(primaryTarget.getOwner()).thenReturn(1);

        Vector2f point = new Vector2f(0f, 0f);
        // Owner 100 (Misc.OWNER_NEUTRAL) differs from source's owner but is not hostile:
        // a bystander in a 3+ side battle should never be dragged into a split.
        ShipAPI neutralBystander = mockBeamSplitEnemy(100, new Vector2f(100f, 0f));

        DamageAPI damage = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(damage.getDamage()).thenReturn(90f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<MagicFakeBeam> ignoredFakeBeam = Mockito.mockStatic(MagicFakeBeam.class);
             MockedStatic<MagicFakeBeamPlugin> ignoredFakeBeamPlugin = Mockito.mockStatic(MagicFakeBeamPlugin.class)) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            globalMock.when(Global::getCombatEngine).thenReturn(engine);
            stubShipGrid(engine, ship, primaryTarget, neutralBystander);

            listener.modifyDamageDealt(beam, primaryTarget, damage, point, false);

            verify(damage.getModifier(), never()).modifyMult(anyString(), anyFloat());
            verify(engine, never()).applyDamage(eq(beam), eq(neutralBystander), any(Vector2f.class), anyFloat(),
                    any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());
        }
    }

    @Test
    void shieldConversionsAndMakeshiftShieldsAreRefusedOnPhaseHulls() {
        com.fs.starfarer.api.combat.ShieldAPI.ShieldType phaseCloak = com.fs.starfarer.api.combat.ShieldAPI.ShieldType.PHASE;
        ShipFacts phase = new ShipFacts(ShipAPI.HullSize.CRUISER, phaseCloak, true, 500f, hullModId -> false);

        for (SkillEffect effect : List.of(ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE, ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT,
                ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI)) {
            assertNotNull(effect.blockAllocationReason(phase, phaseCloak), effect.name());
        }
        assertNull(ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE.blockAllocationReason(ANY_SHIP,
                com.fs.starfarer.api.combat.ShieldAPI.ShieldType.NONE));
        assertEquals(phaseCloak, ShieldSkillEffect.resolveDisplayShieldType(phaseCloak,
                List.of(ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE, ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI)));
    }

    @Test
    void makeshiftFrontShieldIsNeverBuiltOnAPhaseHull() {
        ShipAPI ship = mock(ShipAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getShieldType()).thenReturn(com.fs.starfarer.api.combat.ShieldAPI.ShieldType.PHASE);
        when(ship.getHullSpec()).thenReturn(hullSpec);

        ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE.applyAfterShipCreation(ship, "mod_id", 1f);

        verify(ship, never()).setShield(any(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void maneuverabilityRaisesEveryMovementStatAndDoublesTurnAccelerationLikeVanilla() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat acceleration = new MutableStat(100f);
        MutableStat deceleration = new MutableStat(100f);
        MutableStat turnAcceleration = new MutableStat(100f);
        MutableStat maxTurnRate = new MutableStat(100f);
        when(stats.getAcceleration()).thenReturn(acceleration);
        when(stats.getDeceleration()).thenReturn(deceleration);
        when(stats.getTurnAcceleration()).thenReturn(turnAcceleration);
        when(stats.getMaxTurnRate()).thenReturn(maxTurnRate);

        MovementSkillEffect.MANEUVERABILITY_PERCENT.apply(stats, "mod_id", 20f);

        assertEquals(20f, acceleration.getPercentMod(), 1e-4f);
        assertEquals(20f, deceleration.getPercentMod(), 1e-4f);
        assertEquals(40f, turnAcceleration.getPercentMod(), 1e-4f);
        assertEquals(20f, maxTurnRate.getPercentMod(), 1e-4f);
        assertTrue(MovementSkillEffect.MANEUVERABILITY_PERCENT.supportsTemporaryGating());
    }

    @Test
    void commandPointRecoveryIsDescribedAsAPercentage() {
        assertEquals("250% increased command point recovery rate while this ship is the flagship.",
                MiscSkillEffect.COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP.description(250f).plain());
    }
}
