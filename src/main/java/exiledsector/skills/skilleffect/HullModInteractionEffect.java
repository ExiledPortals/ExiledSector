package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import exiledsector.i18n.Translation;

import java.util.List;
import java.util.function.Supplier;

import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

public enum HullModInteractionEffect implements BackedSkillEffect {

    RANGEFINDER_BALLISTIC_WEAPON_DAMAGE_PERCENT(HullModStatEffect.installed(PERCENT, WeaponStatFamily.DAMAGE.target(WeaponScope.BALLISTIC),
            ballistic("weapon.stat.DAMAGE"), false, HullMods.BALLISTIC_RANGEFINDER)),
    TARGETING_CORE_BALLISTIC_WEAPON_DAMAGE_PERCENT(HullModStatEffect.installed(PERCENT, WeaponStatFamily.DAMAGE.target(WeaponScope.BALLISTIC),
            ballistic("weapon.stat.DAMAGE"), false, HullMods.DEDICATED_TARGETING_CORE)),
    TARGETING_CORE_BALLISTIC_PROJECTILE_SPEED_PERCENT(HullModStatEffect.installed(PERCENT,
            WeaponStatFamily.PROJECTILE_SPEED.target(WeaponScope.BALLISTIC), ballistic("weapon.stat.PROJECTILE_SPEED"), false,
            HullMods.DEDICATED_TARGETING_CORE)),
    FIRE_CONTROL_FLUX_DISSIPATION_MULT(HullModStatEffect.installed(MULT, liveStat(MutableShipStatsAPI::getFluxDissipation),
            named("stat.fluxDissipation"), false, HullMods.DISTRIBUTED_FIRE_CONTROL)),
    POINT_DEFENSE_AI_DAMAGE_TO_FIGHTERS_MULT(HullModStatEffect.installed(MULT, liveStat(MutableShipStatsAPI::getDamageToFighters),
            named("stat.damageToFighters"), false, HullMods.POINTDEFENSEAI)),
    ECM_BALLISTIC_WEAPON_RANGE_PERCENT(HullModStatEffect.installed(PERCENT, WeaponStatFamily.RANGE.target(WeaponScope.BALLISTIC),
            ballistic("weapon.stat.RANGE"), false, HullMods.ECM)),
    NAV_RELAY_TOP_SPEED_PERCENT(HullModStatEffect.installed(PERCENT, liveStat(MutableShipStatsAPI::getMaxSpeed), named("stat.topSpeed"),
            false, HullMods.NAV_RELAY)),
    BULKHEADS_EMP_DAMAGE_TAKEN_MULT(HullModStatEffect.installed(MULT, liveStat(MutableShipStatsAPI::getEmpDamageTakenMult),
            named("stat.empDamageTaken"), true, HullMods.REINFORCEDHULL)),
    BUILT_IN_BLAST_DOORS_HULL_PERCENT(HullModStatEffect.builtIn(PERCENT, bonus(MutableShipStatsAPI::getHullBonus), named("stat.hullPoints"),
            false, HullMods.BLAST_DOORS)),
    BUILT_IN_INSULATED_ENGINE_ACCELERATION_PERCENT(HullModStatEffect.builtIn(PERCENT,
            new StatTarget.Composite(List.of(liveStat(MutableShipStatsAPI::getAcceleration), liveStat(MutableShipStatsAPI::getTurnAcceleration))),
            named("stat.accelerationAndTurnAcceleration"), false, HullMods.INSULATEDENGINE)),
    BUILT_IN_INSULATED_ENGINE_TURN_RATE_PERCENT(HullModStatEffect.builtIn(PERCENT,
            new StatTarget.Composite(List.of(liveStat(MutableShipStatsAPI::getDeceleration), liveStat(MutableShipStatsAPI::getMaxTurnRate))),
            named("stat.decelerationAndTurnRate"), false, HullMods.INSULATEDENGINE)),
    BUILT_IN_ARMORED_MOUNTS_RECOIL_MULT(HullModStatEffect.builtIn(MULT, WeaponStatFamily.RECOIL.target(WeaponScope.ALL),
            named("weapon.stat.RECOIL"), true, HullMods.ARMOREDWEAPONS)),
    BUILT_IN_FUEL_TANKS_TOP_SPEED_PERCENT(HullModStatEffect.builtIn(PERCENT, liveStat(MutableShipStatsAPI::getMaxSpeed), named("stat.topSpeed"),
            false, HullMods.AUXILIARY_FUEL_TANKS)),
    BUILT_IN_BERTHING_PEAK_CR_PERCENT(HullModStatEffect.builtIn(PERCENT, bonus(MutableShipStatsAPI::getPeakCRDuration),
            named("stat.peakCombatReadinessDuration"), false, HullMods.ADDITIONAL_BERTHING)),
    BUILT_IN_SENSORS_DAMAGE_TO_HULL_PERCENT(HullModStatEffect.builtIn(PERCENT, liveStat(MutableShipStatsAPI::getDamageToTargetHullMult),
            named("stat.damageToHull"), false, HullModInteractionEffect.HIGH_RESOLUTION_SENSORS)),
    BUILT_IN_EFFICIENCY_OVERHAUL_CR_LOSS_MULT(HullModStatEffect.builtIn(MULT, bonus(MutableShipStatsAPI::getCRLossPerSecondPercent),
            named("stat.rateOfCombatReadinessLossFromExtendedDeployment"), true, HullMods.EFFICIENCY_OVERHAUL)),
    BUILT_IN_AUX_THRUSTERS_ZERO_FLUX_TOP_SPEED_PERCENT(HullModStatEffect.builtInWhile(PERCENT, liveStat(MutableShipStatsAPI::getMaxSpeed),
            named("stat.topSpeedDuringZeroFluxBoost"), HullMods.AUXILIARY_THRUSTERS, HullModInteractionEffect::zeroFluxBoostActive));

    private static final String HIGH_RESOLUTION_SENSORS = "hiressensors";

    private final EffectBacking backing;

    HullModInteractionEffect(EffectBacking backing) {
        this.backing = backing;
    }

    private static Supplier<String> named(String statKey) {
        return () -> Translation.text(statKey);
    }

    private static Supplier<String> ballistic(String statKey) {
        return () -> WeaponScope.BALLISTIC.qualify(statKey);
    }

    private static boolean zeroFluxBoostActive(ShipAPI ship) {
        return ship.getFluxTracker() != null && ship.getFluxTracker().isEngineBoostActive();
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
