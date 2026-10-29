package exiledsector.ui.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

final class StatRows {

    enum Shape {
        AMOUNT,
        CHANGE,
        POINTS
    }

    record Context(MutableShipStatsAPI stats, ShipHullSpecAPI hullSpec, FleetMemberAPI member) {
    }

    record RowSpec(StatGroup group, String labelKey, Function<Context, StatReading> reader, Shape shape, boolean lowerIsBetter, boolean core) {
    }

    record GridSpec(StatGroup group, List<String> columnKeys, Set<Integer> optionalColumns, List<GridRowSpec> rows) {
    }

    record GridRowSpec(String labelKey, List<Function<Context, StatReading>> cells, boolean lowerIsBetter) {
    }

    private static final List<String> DAMAGE_TYPE_COLUMNS = List.of("ui.stats.column.KINETIC", "ui.stats.column.HIGH_EXPLOSIVE",
            "ui.stats.column.ENERGY_DAMAGE", "ui.stats.column.FRAGMENTATION");
    private static final List<String> WEAPON_COLUMNS = List.of("ui.stats.column.BALLISTIC", "ui.stats.column.ENERGY", "ui.stats.column.MISSILE",
            "ui.stats.column.BEAM");
    private static final List<String> HULL_SIZE_COLUMNS = List.of("ui.stats.column.FRIGATE", "ui.stats.column.DESTROYER",
            "ui.stats.column.CRUISER", "ui.stats.column.CAPITAL");
    private static final List<String> HIT_SOURCE_COLUMNS = List.of("ui.stats.column.PROJECTILE", "ui.stats.column.BEAM",
            "ui.stats.column.MISSILE");
    private static final int BEAM_COLUMN = 3;

    static final List<RowSpec> ROWS = rows();
    static final List<GridSpec> GRIDS = grids();

    private StatRows() {
    }

    private static List<RowSpec> rows() {
        List<RowSpec> rows = new ArrayList<>();
        StatGroup group = StatGroup.DURABILITY;
        rows.add(amount(group, "hullPoints", bonus(MutableShipStatsAPI::getHullBonus, ShipHullSpecAPI::getHitpoints), false, true));
        rows.add(amount(group, "armor", bonus(MutableShipStatsAPI::getArmorBonus, ShipHullSpecAPI::getArmorRating), false, true));
        rows.add(change(group, "hullDamageTaken", stat(MutableShipStatsAPI::getHullDamageTakenMult), true));
        rows.add(change(group, "armorDamageTaken", stat(MutableShipStatsAPI::getArmorDamageTakenMult), true));
        rows.add(change(group, "empDamageTaken", stat(MutableShipStatsAPI::getEmpDamageTakenMult), true));
        rows.add(change(group, "effectiveArmor", unitBonus(MutableShipStatsAPI::getEffectiveArmorBonus), false));
        rows.add(points(group, "minArmorFraction", stat(MutableShipStatsAPI::getMinArmorFraction), false, false));
        rows.add(points(group, "maxArmorReduction", stat(MutableShipStatsAPI::getMaxArmorDamageReduction), false, false));
        rows.add(change(group, "weaponDurability", unitBonus(MutableShipStatsAPI::getWeaponHealthBonus), false));
        rows.add(change(group, "engineDurability", unitBonus(MutableShipStatsAPI::getEngineHealthBonus), false));
        rows.add(change(group, "weaponRepairTime", stat(MutableShipStatsAPI::getCombatWeaponRepairTimeMult), true));
        rows.add(change(group, "engineRepairTime", stat(MutableShipStatsAPI::getCombatEngineRepairTimeMult), true));

        group = StatGroup.SHIELDS;
        rows.add(change(group, "shieldUnfoldRate", stat(MutableShipStatsAPI::getShieldUnfoldRateMult), false));
        rows.add(change(group, "shieldTurnRate", stat(MutableShipStatsAPI::getShieldTurnRateMult), false));
        rows.add(points(group, "shieldSoftFlux", stat(MutableShipStatsAPI::getShieldSoftFluxConversion), false, false));

        group = StatGroup.PHASE;
        rows.add(change(group, "phaseActivationCost", unitBonus(MutableShipStatsAPI::getPhaseCloakActivationCostBonus), true, true));
        rows.add(change(group, "phaseUpkeep", unitBonus(MutableShipStatsAPI::getPhaseCloakUpkeepCostBonus), true, true));
        rows.add(change(group, "phaseCooldown", unitBonus(MutableShipStatsAPI::getPhaseCloakCooldownBonus), true, true));

        group = StatGroup.FLUX;
        rows.add(amount(group, "maxFlux", stat(MutableShipStatsAPI::getFluxCapacity), false, true));
        rows.add(amount(group, "fluxDissipation", stat(MutableShipStatsAPI::getFluxDissipation), false, true));
        rows.add(change(group, "ventRate", stat(MutableShipStatsAPI::getVentRateMult), false));
        rows.add(points(group, "hardFluxDissipation", stat(MutableShipStatsAPI::getHardFluxDissipationFraction), false, false));
        rows.add(change(group, "overloadTime", unitBonus(MutableShipStatsAPI::getOverloadTimeMod), true));
        rows.add(change(group, "fluxDamageTaken", stat(MutableShipStatsAPI::getFluxDamageTakenMult), true));

        group = StatGroup.WEAPONS;
        rows.add(change(group, "weaponTurnRate", unitBonus(MutableShipStatsAPI::getWeaponTurnRateBonus), false));
        rows.add(change(group, "recoil", stat(MutableShipStatsAPI::getMaxRecoilMult), true));
        rows.add(points(group, "autofireAccuracy", stat(MutableShipStatsAPI::getAutofireAimAccuracy), false, false));
        rows.add(amount(group, "rangeThreshold", stat(MutableShipStatsAPI::getWeaponRangeThreshold), false, false));
        rows.add(change(group, "damageToFighters", stat(MutableShipStatsAPI::getDamageToFighters), false));
        rows.add(change(group, "damageToMissiles", stat(MutableShipStatsAPI::getDamageToMissiles), false));
        rows.add(change(group, "damageToShields", stat(MutableShipStatsAPI::getDamageToTargetShieldsMult), false));
        rows.add(change(group, "damageToHull", stat(MutableShipStatsAPI::getDamageToTargetHullMult), false));
        rows.add(change(group, "damageToWeapons", stat(MutableShipStatsAPI::getDamageToTargetWeaponsMult), false));
        rows.add(change(group, "damageToEngines", stat(MutableShipStatsAPI::getDamageToTargetEnginesMult), false));
        rows.add(points(group, "weaponMalfunction", stat(MutableShipStatsAPI::getWeaponMalfunctionChance), true, false));
        rows.add(points(group, "eccmChance", stat(MutableShipStatsAPI::getEccmChance), false, false));
        rows.add(change(group, "missileHealth", unitBonus(MutableShipStatsAPI::getMissileHealthBonus), false));
        rows.add(change(group, "missileManeuverability", unitBonus(MutableShipStatsAPI::getMissileMaxTurnRateBonus), false));

        group = StatGroup.FIGHTERS;
        rows.add(amount(group, "fighterBays", stat(MutableShipStatsAPI::getNumFighterBays), false, true));
        rows.add(change(group, "fighterRefitTime", stat(MutableShipStatsAPI::getFighterRefitTimeMult), true));
        rows.add(change(group, "replacementRateLoss", dynamic(Stats.REPLACEMENT_RATE_DECREASE_MULT), true));
        rows.add(change(group, "replacementRateRegen", dynamic(Stats.REPLACEMENT_RATE_INCREASE_MULT), false));
        rows.add(change(group, "fighterWingRange", unitBonus(MutableShipStatsAPI::getFighterWingRange), false));

        group = StatGroup.MOBILITY;
        rows.add(amount(group, "topSpeed", stat(MutableShipStatsAPI::getMaxSpeed), false, true));
        rows.add(amount(group, "acceleration", stat(MutableShipStatsAPI::getAcceleration), false, true));
        rows.add(amount(group, "deceleration", stat(MutableShipStatsAPI::getDeceleration), false, false));
        rows.add(amount(group, "maxTurnRate", stat(MutableShipStatsAPI::getMaxTurnRate), false, true));
        rows.add(amount(group, "turnAcceleration", stat(MutableShipStatsAPI::getTurnAcceleration), false, false));
        rows.add(amount(group, "zeroFluxBoost", stat(MutableShipStatsAPI::getZeroFluxSpeedBoost), false, false));

        group = StatGroup.SYSTEM;
        rows.add(change(group, "systemCooldown", unitBonus(MutableShipStatsAPI::getSystemCooldownBonus), true));
        rows.add(change(group, "systemRegen", unitBonus(MutableShipStatsAPI::getSystemRegenBonus), false));
        rows.add(change(group, "systemUses", unitBonus(MutableShipStatsAPI::getSystemUsesBonus), false));
        rows.add(change(group, "systemFluxCost", unitBonus(MutableShipStatsAPI::getSystemFluxCostBonus), true));
        rows.add(change(group, "systemRange", unitBonus(MutableShipStatsAPI::getSystemRangeBonus), false));

        group = StatGroup.READINESS;
        rows.add(points(group, "maxCombatReadiness", stat(MutableShipStatsAPI::getMaxCombatReadiness), false, true));
        rows.add(amount(group, "crPerDeployment", bonus(MutableShipStatsAPI::getCRPerDeploymentPercent, ShipHullSpecAPI::getCRToDeploy), true, false));
        rows.add(amount(group, "peakPerformanceTime", bonus(MutableShipStatsAPI::getPeakCRDuration, ShipHullSpecAPI::getNoCRLossSeconds), false, true));
        rows.add(change(group, "crLossRate", unitBonus(MutableShipStatsAPI::getCRLossPerSecondPercent), true));
        rows.add(amount(group, "crRecovery", stat(MutableShipStatsAPI::getBaseCRRecoveryRatePercentPerDay), false, false));
        rows.add(amount(group, "repairRate", stat(MutableShipStatsAPI::getRepairRatePercentPerDay), false, false));

        group = StatGroup.LOGISTICS;
        rows.add(amount(group, "cargoCapacity", bonus(MutableShipStatsAPI::getCargoMod, ShipHullSpecAPI::getCargo), false, true));
        rows.add(amount(group, "fuelCapacity", bonus(MutableShipStatsAPI::getFuelMod, ShipHullSpecAPI::getFuel), false, true));
        rows.add(amount(group, "fuelUse", bonus(MutableShipStatsAPI::getFuelUseMod, ShipHullSpecAPI::getFuelPerLY), true, true));
        rows.add(amount(group, "minCrew", bonus(MutableShipStatsAPI::getMinCrewMod, ShipHullSpecAPI::getMinCrew), true, false));
        rows.add(amount(group, "maxCrew", bonus(MutableShipStatsAPI::getMaxCrewMod, ShipHullSpecAPI::getMaxCrew), false, false));
        rows.add(amount(group, "burnLevel", stat(MutableShipStatsAPI::getMaxBurnLevel), false, true));
        rows.add(amount(group, "sensorProfile", stat(MutableShipStatsAPI::getSensorProfile), true, true));
        rows.add(amount(group, "sensorStrength", stat(MutableShipStatsAPI::getSensorStrength), false, true));
        rows.add(change(group, "sightRadius", unitBonus(MutableShipStatsAPI::getSightRadiusMod), false));
        rows.add(amount(group, "suppliesPerMonth", stat(MutableShipStatsAPI::getSuppliesPerMonth), true, true));
        rows.add(amount(group, "suppliesToRecover", stat(MutableShipStatsAPI::getSuppliesToRecover), true, false));
        rows.add(change(group, "crewLoss", stat(MutableShipStatsAPI::getCrewLossMult), true));
        return Collections.unmodifiableList(rows);
    }

    private static List<GridSpec> grids() {
        List<GridSpec> grids = new ArrayList<>();
        grids.add(new GridSpec(StatGroup.DURABILITY, DAMAGE_TYPE_COLUMNS, Set.of(), List.of(
                gridRow("damageTaken", true, stat(MutableShipStatsAPI::getKineticDamageTakenMult),
                        stat(MutableShipStatsAPI::getHighExplosiveDamageTakenMult), stat(MutableShipStatsAPI::getEnergyDamageTakenMult),
                        stat(MutableShipStatsAPI::getFragmentationDamageTakenMult)),
                gridRow("armorDamageTakenByType", true, stat(MutableShipStatsAPI::getKineticArmorDamageTakenMult), null, null, null))));
        grids.add(new GridSpec(StatGroup.DURABILITY, HIT_SOURCE_COLUMNS, Set.of(), List.of(
                gridRow("damageTaken", true, stat(MutableShipStatsAPI::getProjectileDamageTakenMult),
                        stat(MutableShipStatsAPI::getBeamDamageTakenMult), stat(MutableShipStatsAPI::getMissileDamageTakenMult)))));
        grids.add(new GridSpec(StatGroup.SHIELDS, DAMAGE_TYPE_COLUMNS, Set.of(), List.of(
                gridRow("shieldDamageTaken", true, stat(MutableShipStatsAPI::getKineticShieldDamageTakenMult),
                        stat(MutableShipStatsAPI::getHighExplosiveShieldDamageTakenMult), stat(MutableShipStatsAPI::getEnergyShieldDamageTakenMult),
                        stat(MutableShipStatsAPI::getFragmentationShieldDamageTakenMult)))));
        grids.add(new GridSpec(StatGroup.SHIELDS, HIT_SOURCE_COLUMNS, Set.of(), List.of(
                gridRow("shieldDamageTaken", true, stat(MutableShipStatsAPI::getProjectileShieldDamageTakenMult),
                        stat(MutableShipStatsAPI::getBeamShieldDamageTakenMult), stat(MutableShipStatsAPI::getMissileShieldDamageTakenMult)))));
        grids.add(new GridSpec(StatGroup.WEAPONS, WEAPON_COLUMNS, Set.of(BEAM_COLUMN), List.of(
                gridRow("weaponDamage", false, stat(MutableShipStatsAPI::getBallisticWeaponDamageMult),
                        stat(MutableShipStatsAPI::getEnergyWeaponDamageMult), stat(MutableShipStatsAPI::getMissileWeaponDamageMult),
                        stat(MutableShipStatsAPI::getBeamWeaponDamageMult)),
                gridRow("weaponRange", false, unitBonus(MutableShipStatsAPI::getBallisticWeaponRangeBonus),
                        unitBonus(MutableShipStatsAPI::getEnergyWeaponRangeBonus), unitBonus(MutableShipStatsAPI::getMissileWeaponRangeBonus),
                        unitBonus(MutableShipStatsAPI::getBeamWeaponRangeBonus)),
                gridRow("weaponFluxCost", true, unitBonus(MutableShipStatsAPI::getBallisticWeaponFluxCostMod),
                        unitBonus(MutableShipStatsAPI::getEnergyWeaponFluxCostMod), unitBonus(MutableShipStatsAPI::getMissileWeaponFluxCostMod),
                        stat(MutableShipStatsAPI::getBeamWeaponFluxCostMult)),
                gridRow("weaponRateOfFire", false, stat(MutableShipStatsAPI::getBallisticRoFMult), stat(MutableShipStatsAPI::getEnergyRoFMult),
                        stat(MutableShipStatsAPI::getMissileRoFMult), null),
                gridRow("weaponAmmo", false, unitBonus(MutableShipStatsAPI::getBallisticAmmoBonus), unitBonus(MutableShipStatsAPI::getEnergyAmmoBonus),
                        unitBonus(MutableShipStatsAPI::getMissileAmmoBonus), null),
                gridRow("weaponAmmoRegen", false, stat(MutableShipStatsAPI::getBallisticAmmoRegenMult),
                        stat(MutableShipStatsAPI::getEnergyAmmoRegenMult), stat(MutableShipStatsAPI::getMissileAmmoRegenMult), null),
                gridRow("projectileSpeed", false, stat(MutableShipStatsAPI::getBallisticProjectileSpeedMult),
                        stat(MutableShipStatsAPI::getEnergyProjectileSpeedMult), unitBonus(MutableShipStatsAPI::getMissileMaxSpeedBonus),
                        unitBonus(MutableShipStatsAPI::getBeamSpeedMod)))));
        grids.add(new GridSpec(StatGroup.WEAPONS, HULL_SIZE_COLUMNS, Set.of(), List.of(
                gridRow("damageDealt", false, stat(MutableShipStatsAPI::getDamageToFrigates), stat(MutableShipStatsAPI::getDamageToDestroyers),
                        stat(MutableShipStatsAPI::getDamageToCruisers), stat(MutableShipStatsAPI::getDamageToCapital)))));
        return Collections.unmodifiableList(grids);
    }

    private static RowSpec amount(StatGroup group, String key, Function<Context, StatReading> reader, boolean lowerIsBetter, boolean core) {
        return new RowSpec(group, "ui.stats." + key, reader, Shape.AMOUNT, lowerIsBetter, core);
    }

    private static RowSpec change(StatGroup group, String key, Function<Context, StatReading> reader, boolean lowerIsBetter) {
        return change(group, key, reader, lowerIsBetter, false);
    }

    private static RowSpec change(StatGroup group, String key, Function<Context, StatReading> reader, boolean lowerIsBetter, boolean core) {
        return new RowSpec(group, "ui.stats." + key, reader, Shape.CHANGE, lowerIsBetter, core);
    }

    private static RowSpec points(StatGroup group, String key, Function<Context, StatReading> reader, boolean lowerIsBetter, boolean core) {
        return new RowSpec(group, "ui.stats." + key, reader, Shape.POINTS, lowerIsBetter, core);
    }

    @SafeVarargs
    private static GridRowSpec gridRow(String key, boolean lowerIsBetter, Function<Context, StatReading>... cells) {
        return new GridRowSpec("ui.stats." + key, Collections.unmodifiableList(Arrays.asList(cells)), lowerIsBetter);
    }

    private static Function<Context, StatReading> stat(Function<MutableShipStatsAPI, MutableStat> getter) {
        return context -> {
            MutableStat stat = getter.apply(context.stats());
            return stat == null ? null : StatReading.of(stat);
        };
    }

    private static Function<Context, StatReading> dynamic(String statId) {
        return context -> {
            MutableStat stat = context.stats().getDynamic() == null ? null : context.stats().getDynamic().getStat(statId);
            return stat == null ? null : StatReading.of(stat);
        };
    }

    private static Function<Context, StatReading> unitBonus(Function<MutableShipStatsAPI, StatBonus> getter) {
        return context -> {
            StatBonus bonus = getter.apply(context.stats());
            return bonus == null ? null : StatReading.of(bonus, 1f);
        };
    }

    private static Function<Context, StatReading> bonus(Function<MutableShipStatsAPI, StatBonus> getter, ToDoubleFunction<ShipHullSpecAPI> base) {
        return context -> {
            StatBonus bonus = getter.apply(context.stats());
            return bonus == null ? null : StatReading.of(bonus, (float) base.applyAsDouble(context.hullSpec()));
        };
    }
}
