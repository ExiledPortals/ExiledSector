package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledlib.compat.LostSectorCompat;
import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.ListenerEffect;
import exiledlib.skills.skilleffect.PerDModMultEffect;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;

import java.util.function.Function;

import static exiledlib.skills.skilleffect.StatMode.PERCENT;
import static exiledlib.skills.skilleffect.StatTarget.stat;

public enum WeaponSkillEffect implements BackedSkillEffect {

    WEAPON_DAMAGE_MULT_PER_DMOD(new PerDModMultEffect(WeaponStatFamily.DAMAGE.target(WeaponScope.ALL), "stat.weaponDamagePerDMod", false)),
    BALLISTIC_WEAPON_DAMAGE_PER_BURN_LEVEL_MULT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            applyBurnDamage(stats, modId, magnitude);
        }

        @Override
        public boolean appliesAfterOtherEffects() {
            return true;
        }

        @Override
        public StatMode statMode() {
            return StatMode.MULT;
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            applyBurnDamage(ship.getMutableStats(), modId, magnitude);
        }
    },
    ENERGY_WEAPON_RANGE_PER_SENSOR_STRENGTH_FLAT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            applySensorRange(stats, modId, magnitude);
        }

        @Override
        public boolean appliesAfterOtherEffects() {
            return true;
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            applySensorRange(ship.getMutableStats(), modId, magnitude);
        }
    },
    MISSILE_RELOAD_PERCENT_PER_MINUTE {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public boolean advancesInCombat() {
            return true;
        }

        @Override
        public void advanceInCombat(ShipAPI ship, String modId, float magnitude, float amount) {
            MissileReloader.advance(ship, modId, magnitude, amount);
        }
    },
    BALLISTIC_WEAPON_LARGE_OP_COST_FLAT {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(LARGE_BALLISTIC_OP_COST_KEY).modifyFlat(modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.FLAT.describeStat(magnitude, "stat.ordnancePointCostOfLargeBallisticWeapons");
        }
    },
    DAMAGE_TO_FRIGATE_PERCENT(PERCENT, stat(MutableShipStatsAPI::getDamageToFrigates), "stat.damageToFrigates", false),
    DAMAGE_TO_DESTROYER_PERCENT(PERCENT, stat(MutableShipStatsAPI::getDamageToDestroyers), "stat.damageToDestroyers", false),
    DAMAGE_TO_CRUISER_PERCENT(PERCENT, stat(MutableShipStatsAPI::getDamageToCruisers), "stat.damageToCruisers", false),
    DAMAGE_TO_CAPITAL_PERCENT(PERCENT, stat(MutableShipStatsAPI::getDamageToCapital), "stat.damageToCapitalShips", false),
    WEAPON_MALFUNCTION_PERCENT(PERCENT, new StatTarget.PercentagePoints(MutableShipStatsAPI::getWeaponMalfunctionChance),
            "stat.weaponMalfunctionChance", true),
    PD_IGNORES_DECOY_FLARES {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(modId, 1f);
        }
    },
    PD_BEST_TARGET_LEADING {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(modId, 1f);
        }
    },
    PD_DAMAGE_TO_MISSILES_PERCENT(PERCENT, stat(MutableShipStatsAPI::getDamageToMissiles),
            "stat.damageDealtToMissilesByPointDefenceWeapons", false),
    PD_RECLASSIFY_SMALL_WEAPONS {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                boolean sizeMatches = weapon.getSize() == WeaponSize.SMALL;
                if (sizeMatches && weapon.getType() != WeaponType.MISSILE && !weapon.hasAIHint(AIHints.STRIKE)) {
                    weapon.setPD(true);
                }
            }
        }
    },
    BALLISTIC_COUNTS_AS_ENERGY {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(BallisticAsEnergy.KEY).modifyFlat(modId, 1f);
        }
    },
    BALLISTIC_SLOTS_HYBRID {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            HybridBallisticSlots.convert(stats.getVariant());
        }
    },
    MEDIUM_ENERGY_SLOT_WEAPON_RANGE_FLAT(MediumEnergySlotRangeListener.RANGE_FLAT_KEY,
            MediumEnergySlotRangeListener.class, ship -> new MediumEnergySlotRangeListener()),
    NON_BEAM_WEAPON_DAMAGE_PER_SPEED_PERCENT(InertialSuperchargerListener.DAMAGE_PERCENT_PER_SPEED_KEY,
            InertialSuperchargerListener.class, InertialSuperchargerListener::new),
    AUGMENTED_INERTIAL_PROJECTILE_SPEED_PERCENT(InertialSuperchargerListener.AUGMENTED_PROJECTILE_SPEED_KEY,
            InertialSuperchargerListener.class, InertialSuperchargerListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return LostSectorCompat.isModEnabled() ? super.description(magnitude) : null;
        }
    },
    FUROR_RATE_OF_FIRE_PERCENT(FurorListener.RATE_OF_FIRE_PERCENT_KEY, FurorListener.class, FurorListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return FurorListener.DAMAGE_FOR_FULL_FUROR.withShipSizeArgs(EffectText.msg(this).arg("value", magnitude))
                    .arg("decay", FurorListener.DECAY_PERCENT_PER_SECOND).styled();
        }
    },
    THERMO_ACCELERATOR_FLUX_COST_REDUCTION_PERCENT(ThermoAcceleratorListener.FLUX_COST_REDUCTION_PERCENT_KEY, ThermoAcceleratorListener.class,
            ThermoAcceleratorListener::new),
    SOLARIS_OVERDRIVE_SECONDS_FLAT(SolarisOverdriveListener.SECONDS_KEY, SolarisOverdriveListener.class, SolarisOverdriveListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("cooldown", SolarisOverdriveListener.COOLDOWN_SECONDS).styled();
        }
    },
    SOLARIS_OVERDRIVE_PROJECTILE_SPEED_PERCENT(SolarisOverdriveListener.PROJECTILE_SPEED_PERCENT_KEY, SolarisOverdriveListener.class,
            SolarisOverdriveListener::new),
    SOLARIS_OVERDRIVE_RATE_OF_FIRE_PERCENT(SolarisOverdriveListener.RATE_OF_FIRE_PERCENT_KEY, SolarisOverdriveListener.class,
            SolarisOverdriveListener::new);

    private static final String LARGE_BALLISTIC_OP_COST_KEY = "large_ballistic_mod";

    private final EffectBacking backing;

    WeaponSkillEffect() {
        this((EffectBacking) null);
    }

    WeaponSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    <T> WeaponSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this(new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory));
    }

    WeaponSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    private static void applyBurnDamage(MutableShipStatsAPI stats, String modId, float magnitude) {
        MutableStat burnLevel = stats.getMaxBurnLevel();
        float burnOverDefault = Math.max(0f, burnLevel.getModifiedValue() - burnLevel.getBaseValue());
        WeaponStatFamily.DAMAGE.target(WeaponScope.BALLISTIC).apply(stats, modId, StatMode.MULT, burnOverDefault * magnitude);
    }

    private static void applySensorRange(MutableShipStatsAPI stats, String modId, float magnitude) {
        float rangeBonus = stats.getSensorStrength().getModifiedValue() * magnitude;
        WeaponStatFamily.RANGE.target(WeaponScope.ENERGY).apply(stats, modId, StatMode.FLAT, rangeBonus);
        if (BallisticAsEnergy.active(stats)) {
            WeaponStatFamily.RANGE.target(WeaponScope.BALLISTIC).apply(stats, modId, StatMode.FLAT, rangeBonus);
        } else {
            WeaponStatFamily.RANGE.target(WeaponScope.BALLISTIC).remove(stats, modId);
        }
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
