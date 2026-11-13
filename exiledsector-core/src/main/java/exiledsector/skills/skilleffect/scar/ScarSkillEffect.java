package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.i18n.StyledText;
import exiledsector.skills.CombatGate;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.movement.Maneuverability;
import exiledsector.skills.skilleffect.readiness.ReadinessSkillEffect;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponSkillEffect;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

import static exiledsector.skills.skilleffect.Backings.listener;
import static exiledsector.skills.skilleffect.Backings.statEffect;
import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicMod;
import static exiledsector.skills.skilleffect.StatTarget.liveBonus;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum ScarSkillEffect implements BackedSkillEffect {

    SCAR_ARMOR_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getArmorBonus), "stat.armor", false)),
    SCAR_HULL_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getHullBonus), "stat.hullPoints", false)),
    SCAR_HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, all(liveStat(MutableShipStatsAPI::getHighExplosiveDamageTakenMult),
            liveStat(MutableShipStatsAPI::getHighExplosiveShieldDamageTakenMult)), "stat.highExplosiveDamageTaken", true)),
    SCAR_KINETIC_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, all(liveStat(MutableShipStatsAPI::getKineticDamageTakenMult),
            liveStat(MutableShipStatsAPI::getKineticShieldDamageTakenMult)), "stat.kineticDamageTaken", true)),
    SCAR_ENERGY_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, all(liveStat(MutableShipStatsAPI::getEnergyDamageTakenMult),
            liveStat(MutableShipStatsAPI::getEnergyShieldDamageTakenMult)), "stat.energyDamageTaken", true)),
    SCAR_FRAGMENTATION_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, all(liveStat(MutableShipStatsAPI::getFragmentationDamageTakenMult),
            liveStat(MutableShipStatsAPI::getFragmentationShieldDamageTakenMult)), "stat.fragmentationDamageTaken", true)),
    SCAR_EMP_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getEmpDamageTakenMult), "stat.empDamageTaken", true)),
    SCAR_MISSILE_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, all(liveStat(MutableShipStatsAPI::getMissileDamageTakenMult),
            liveStat(MutableShipStatsAPI::getMissileShieldDamageTakenMult)), "stat.missileDamageTaken", true)),
    SCAR_BEAM_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, all(liveStat(MutableShipStatsAPI::getBeamDamageTakenMult),
            liveStat(MutableShipStatsAPI::getBeamShieldDamageTakenMult)), "stat.beamDamageTaken", true)),
    SCAR_MINE_DAMAGE_TAKEN_MULT(listener(MineDamageTakenListener.REDUCTION_PERCENT_KEY, MineDamageTakenListener.class,
            MineDamageTakenListener::new)),
    SCAR_COLLISION_DAMAGE_TAKEN_MULT(listener(CollisionDamageTakenListener.REDUCTION_PERCENT_KEY, CollisionDamageTakenListener.class,
            CollisionDamageTakenListener::new)),
    SCAR_SHIELD_DAMAGE_TAKEN_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getShieldDamageTakenMult), "stat.damageTakenByShields",
            true)),
    SCAR_SHIELD_ARC_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getShieldArcBonus), "stat.shieldArc", false)),
    SCAR_SHIELD_TURN_RATE_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getShieldTurnRateMult), "stat.shieldTurnRate", false)),
    SCAR_SHIELD_DAMAGE_TAKEN_FROM_AUTOMATED_MULT(ShieldSkillEffect.SHIELD_DAMAGE_TAKEN_FROM_AUTOMATED_MULT.backing()),
    SCAR_DAMAGE_TAKEN_NEAR_ALLIES_PERCENT(ConditionalScarStat.damageTakenWhile(AllyProximity::hasAllyNearby, "range", AllyProximity.ALLY_RANGE)),
    SCAR_DAMAGE_TAKEN_AT_LOW_HULL_PERCENT(ConditionalScarStat.damageTakenWhile(CombatGate.LOW_HULL::holds, "hull",
            CombatGate.LOW_HULL_LEVEL * 100f)),
    SCAR_DAMAGE_AND_SPEED_WHILE_ALONE_PERCENT(ConditionalScarStat.raisedWhile(all(WeaponStatFamily.DAMAGE.target(WeaponScope.ALL),
            liveStat(MutableShipStatsAPI::getMaxSpeed)), ship -> !AllyProximity.hasAllyNearby(ship), "range", AllyProximity.ALLY_RANGE)),
    SCAR_ENGINE_DURABILITY_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getEngineHealthBonus), "stat.engineDurability", false)),
    SCAR_WEAPON_DURABILITY_PERCENT(new ScarWeaponStat(WeaponStatFamily.DURABILITY)),
    SCAR_FLUX_CAPACITY_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getFluxCapacity), "stat.fluxCapacity", false)),
    SCAR_FLUX_DISSIPATION_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getFluxDissipation), "stat.fluxDissipation", false)),
    SCAR_HARD_FLUX_DISSIPATION_PERCENT(statEffect(PERCENT, new StatTarget.PercentagePoints(MutableShipStatsAPI::getHardFluxDissipationFraction),
            "stat.hardFluxDissipationWhileShieldsUp", false)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.templated(this, magnitude);
        }
    },
    SCAR_VENT_RATE_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeed", false)),
    SCAR_OVERLOAD_DURATION_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getOverloadTimeMod), "stat.overloadDuration", true)),
    SCAR_ZERO_FLUX_SPEED_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getZeroFluxSpeedBoost), "stat.scar.zeroFluxSpeedBoost", false)),
    SCAR_TOP_SPEED_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getMaxSpeed), "stat.topSpeed", false)),
    SCAR_MANEUVERABILITY_PERCENT(statEffect(PERCENT, Maneuverability.target(), "stat.maneuverability", false)),
    SCAR_PEAK_CR_DURATION_PERCENT(statEffect(PERCENT, liveBonus(MutableShipStatsAPI::getPeakCRDuration), "stat.peakCombatReadinessDuration",
            false)),
    SCAR_MAX_COMBAT_READINESS_PERCENT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            ReadinessSkillEffect.MAX_COMBAT_READINESS_PERCENT.apply(stats, modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }
    },
    SCAR_WEAPON_MALFUNCTION_PERCENT(statEffect(PERCENT, new StatTarget.PercentagePoints(MutableShipStatsAPI::getWeaponMalfunctionChance),
            "stat.weaponMalfunctionChance", true)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }

        @Override
        public boolean invertsDescriptionColours() {
            return false;
        }
    },
    SCAR_SYSTEM_COOLDOWN_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getSystemCooldownBonus), "stat.shipSystemCooldown", true)),
    SCAR_SENSOR_STRENGTH_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getSensorStrength), "stat.scar.sensorStrength", false)),
    SCAR_ELECTRONIC_WARFARE_PENALTY_PERCENT(statEffect(PERCENT, dynamicMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD), "stat.scar.ecmRangePenalty", true)),
    SCAR_NAV_RATING_PERCENT(statEffect(FLAT, dynamicMod("coord_maneuvers_flat"), "stat.fleetNavRating", false)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }
    },
    SCAR_OBJECTIVE_CAPTURE_RANGE_PERCENT(statEffect(PERCENT, dynamicMod(Stats.SHIP_OBJECTIVE_CAP_RANGE_MOD), "stat.objectiveCaptureRange", false)),
    SCAR_WEAPON_RANGE_PERCENT(new ScarWeaponStat(WeaponStatFamily.RANGE)),
    SCAR_PROJECTILE_SPEED_PERCENT(new ScarWeaponStat(WeaponStatFamily.PROJECTILE_SPEED)),
    SCAR_AUTOFIRE_ACCURACY_PERCENT(new ScarWeaponStat(WeaponStatFamily.AUTOFIRE_ACCURACY)),
    SCAR_PD_DAMAGE_TO_MISSILES_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getDamageToMissiles), "stat.pointDefenseDamageToMissiles",
            false)),
    SCAR_DAMAGE_TO_FIGHTERS_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getDamageToFighters), "stat.scar.damageToStrikecraft", false)),
    SCAR_WING_DAMAGE_TO_FIGHTERS_PERCENT {
        @Override
        public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
            fighter.getMutableStats().getDamageToFighters().modifyPercent(modId, magnitude);
        }

        @Override
        public StatMode statMode() {
            return PERCENT;
        }
    },
    SCAR_DAMAGE_TO_FRIGATES_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getDamageToFrigates), "stat.damageToFrigates", false)),
    SCAR_DAMAGE_TO_DESTROYERS_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getDamageToDestroyers), "stat.damageToDestroyers", false)),
    SCAR_DAMAGE_TO_CRUISERS_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getDamageToCruisers), "stat.damageToCruisers", false)),
    SCAR_DAMAGE_TO_CAPITALS_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getDamageToCapital), "stat.damageToCapitalShips", false)),
    SCAR_DAMAGE_TO_SHIELDS_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getDamageToTargetShieldsMult), "stat.damageToShields",
            false)),
    SCAR_DAMAGE_TO_AUTOMATED_SHIPS_PERCENT(WeaponSkillEffect.DAMAGE_TO_AUTOMATED_SHIP_PERCENT.backing()),
    SCAR_DAMAGE_TO_PRISTINE_SHIPS_PERCENT(WeaponSkillEffect.DAMAGE_TO_PRISTINE_SHIP_PERCENT.backing()),
    SCAR_DAMAGE_TO_DMODDED_SHIPS_PERCENT(listener(DModdedShipDamageListener.DAMAGE_PERCENT_KEY, DModdedShipDamageListener.class,
            DModdedShipDamageListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return PERCENT.describeStat(magnitude, "stat.damageToDModdedShips");
        }
    },
    SCAR_DAMAGE_TO_PLAYER_FLAGSHIP_PERCENT(listener(PlayerFlagshipDamageListener.DAMAGE_PERCENT_KEY, PlayerFlagshipDamageListener.class,
            PlayerFlagshipDamageListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return PERCENT.describeStat(magnitude, "stat.damageToYourFlagship");
        }
    },
    SCAR_HIGH_EXPLOSIVE_CONVERSION_CHANCE_PERCENT(listener(HighExplosiveConversionListener.CHANCE_PERCENT_KEY,
            HighExplosiveConversionListener.class, HighExplosiveConversionListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    };

    private final EffectBacking backing;

    ScarSkillEffect() {
        this(null);
    }

    ScarSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public boolean invertsDescriptionColours() {
        return !(backing instanceof ConditionalScarStat) && lowerIsBetter();
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
