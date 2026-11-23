package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.compat.LostSectorCompat;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.PerDModMultEffect;
import exiledsector.skills.skilleffect.special.PerShroudedPercentEffect;

import static exiledsector.skills.skilleffect.Backings.listener;
import static exiledsector.skills.skilleffect.Backings.statEffect;
import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicStat;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum FluxSkillEffect implements BackedSkillEffect {

    FLUX_CAPACITY_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false)),
    FLUX_CAPACITY_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false)),
    FLUX_CAPACITY_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false)),
    FLUX_DISSIPATION_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false)),
    FLUX_DISSIPATION_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false)),
    FLUX_DISSIPATION_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false)),
    HARD_FLUX_DISSIPATION_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getHardFluxDissipationFraction),
            "stat.hardFluxDissipationWhileShieldsUp", false)),
    HARD_FLUX_DISSIPATION_PERCENT(statEffect(PERCENT, new StatTarget.PercentagePoints(MutableShipStatsAPI::getHardFluxDissipationFraction),
            "stat.hardFluxDissipationWhileShieldsUp", false)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.templated(this, magnitude);
        }
    },
    FLUX_DISSIPATION_MULT_PER_DMOD(new PerDModMultEffect(liveStat(MutableShipStatsAPI::getFluxDissipation), "stat.fluxDissipationPerDMod", false)),
    VENT_RATE_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeed", false)),
    VENT_RATE_PERCENT_PER_SHROUDED(new PerShroudedPercentEffect(stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeedPerShrouded", false)),
    VENT_RATE_MULT(statEffect(MULT, stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeed", false)),
    ACTIVE_VENTING_DISABLED {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getVentRateMult().modifyMult(modId, 0f);
        }
    },
    OVERLOAD_DURATION_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getOverloadTimeMod), "stat.overloadDuration", true)),
    ZERO_FLUX_SPEED_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getZeroFluxSpeedBoost), "stat.zeroFluxSpeedBoost", false)),
    ZERO_FLUX_TURN_RATE_MULT(statEffect(MULT, dynamicStat(Stats.ZERO_FLUX_BOOST_TURN_RATE_BONUS_MULT), "stat.zeroFluxTurnRateBonus", false)),
    ZERO_FLUX_ALWAYS_ON {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getZeroFluxMinimumFluxLevel().modifyFlat(modId, 2f);
        }
    },
    ZERO_FLUX_BOOST_FLUX_LEVEL_PERCENT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getZeroFluxMinimumFluxLevel().modifyFlat(modId, magnitude / 100f);
        }
    },
    FLUX_DISSIPATION_WHILE_VENTING_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getFluxDissipation),
            "stat.fluxDissipationWhileVenting", ship -> ship.getFluxTracker().isVenting())),
    FLUX_DISSIPATION_PERCENT_PER_HARD_FLUX(listener(HardFluxDissipationListener.PERCENT_PER_STEP_KEY, HardFluxDissipationListener.class,
            HardFluxDissipationListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("step", HardFluxDissipationListener.HARD_FLUX_STEP_PERCENT).styled();
        }
    },
    HIGH_FLUX_HULL_REGEN_PERCENT(listener(HighFluxMendingListener.REGEN_PERCENT_KEY, HighFluxMendingListener.class,
            HighFluxMendingListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("threshold", HighFluxMendingListener.FLUX_LEVEL_THRESHOLD_PERCENT).arg("value", magnitude)
                    .arg("limit", HighFluxMendingListener.MAX_TOTAL_REGEN_PERCENT_OF_HULL).styled();
        }
    },
    OVERLOAD_HULL_DAMAGE_PERCENT(listener(OverloadHullDamageListener.HULL_DAMAGE_PERCENT_KEY, OverloadHullDamageListener.class,
            OverloadHullDamageListener::new)) {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public boolean invertsDescriptionColours() {
            return false;
        }
    },
    OVERLOAD_HARD_FLUX_DISSIPATION_PERCENT(listener(OverloadHullDamageListener.HARD_FLUX_DISSIPATION_PERCENT_KEY,
            OverloadHullDamageListener.class, OverloadHullDamageListener::new)),
    FLUX_DISSIPATION_WITHOUT_HARD_FLUX_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getFluxDissipation),
            "stat.fluxDissipationWithoutHardFlux", ship -> ship.getFluxTracker().getHardFlux() <= 0f)),
    FLUX_DISSIPATION_PER_FLUX_VENT_FLAT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            ShipVariantAPI variant = stats.getVariant();
            int vents = variant == null ? 0 : variant.getNumFluxVents();
            stats.getFluxDissipation().modifyFlat(modId + VENT_MOD_SUFFIX, vents * magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return FLAT.describeStat(magnitude, "stat.fluxDissipationPerFluxVent");
        }
    },
    FLUX_CAPACITOR_EFFECTIVENESS_PERCENT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            ShipVariantAPI variant = stats.getVariant();
            int capacitors = variant == null ? 0 : variant.getNumFluxCapacitors();
            stats.getFluxCapacity().modifyFlat(modId + CAPACITOR_MOD_SUFFIX, capacitors * fluxPerCapacitor() * magnitude / 100f);
        }
    },
    EMP_DAMAGE_TAKEN_HARD_FLUX_VENT_PERCENT(listener(EmpHardFluxVentListener.VENT_PERCENT_KEY, EmpHardFluxVentListener.class,
            EmpHardFluxVentListener::new)),
    EMP_DEALT_HARD_FLUX_VENT_PERCENT(listener(EmpDealtHardFluxVentListener.VENT_PERCENT_KEY, EmpDealtHardFluxVentListener.class,
            EmpDealtHardFluxVentListener::new)),
    VOLATILE_FLUX_TOP_SPEED_FLAT(listener(VolatileFluxListener.TOP_SPEED_KEY,
            VolatileFluxListener.class, VolatileFluxListener::new)),
    VOLATILE_FLUX_RATE_OF_FIRE_PERCENT(listener(VolatileFluxListener.RATE_OF_FIRE_KEY,
            VolatileFluxListener.class, VolatileFluxListener::new)),
    FLUX_LEVEL_WEAPON_DAMAGE_MULT(listener(FluxOverdriveListener.DAMAGE_PERCENT_KEY, FluxOverdriveListener.class,
            FluxOverdriveListener::new)),
    FLUX_LEVEL_WEAPON_FLUX_COST_MULT(listener(FluxOverdriveListener.FLUX_COST_PERCENT_KEY, FluxOverdriveListener.class,
            FluxOverdriveListener::new)) {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public boolean invertsDescriptionColours() {
            return false;
        }
    },
    FLUX_GENERATOR_FLUX_LEVEL_PERCENT(listener(FluxGeneratorListener.FLUX_LEVEL_PERCENT_KEY, FluxGeneratorListener.class,
            FluxGeneratorListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("multiple", Math.round(FluxGeneratorListener.DISSIPATION_MULTIPLE)).styled();
        }
    },
    FLUX_BOOSTER_VENT_RATE_PERCENT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            ShipVariantAPI variant = stats.getVariant();
            float share = variant != null && variant.hasHullMod(RESISTANT_FLUX_CONDUITS) ? RESISTANT_CONDUITS_VENT_SHARE : 1f;
            stats.getVentRateMult().modifyPercent(modId, magnitude * share);
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("reduced", magnitude * RESISTANT_CONDUITS_VENT_SHARE).styled();
        }
    },
    FLUX_BOOSTER_MANEUVERABILITY_PERCENT(listener(FluxBoosterListener.MANEUVERABILITY_PERCENT_KEY, FluxBoosterListener.class,
            FluxBoosterListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("acceleration", magnitude * FluxBoosterListener.ACCELERATION_SHARE).styled();
        }
    },
    FLUX_BOOSTER_SECONDS_FLAT(listener(FluxBoosterListener.SECONDS_KEY, FluxBoosterListener.class, FluxBoosterListener::new)),
    EMERGENCY_VENTING_OVERLOAD_SECONDS_FLAT(listener(EmergencyVentingListener.OVERLOAD_SECONDS_KEY, EmergencyVentingListener.class,
            EmergencyVentingListener::new)) {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public boolean invertsDescriptionColours() {
            return false;
        }
    },
    EMERGENCY_VENT_RATE_PERCENT(listener(EmergencyVentingListener.VENT_RATE_PERCENT_KEY, EmergencyVentingListener.class,
            EmergencyVentingListener::new)),
    AUGMENTED_FLUX_SCALED_PENALTY_REDUCTION_PERCENT(listener(VolatileFluxListener.AUGMENTED_PENALTY_REDUCTION_KEY,
            VolatileFluxListener.class, VolatileFluxListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return LostSectorCompat.isModEnabled() ? super.description(magnitude) : null;
        }
    };

    static final String RESISTANT_FLUX_CONDUITS = "fluxbreakers";
    static final float RESISTANT_CONDUITS_VENT_SHARE = 0.5f;
    private static final String CAPACITOR_MOD_SUFFIX = "_capacitors";
    private static final String VENT_MOD_SUFFIX = "_vents";
    private static final String FLUX_PER_CAPACITOR_SETTING = "fluxPerCapacitor";
    private static final float DEFAULT_FLUX_PER_CAPACITOR = 200f;

    private static float fluxPerCapacitor() {
        SettingsAPI settings = Global.getSettings();
        return settings == null ? DEFAULT_FLUX_PER_CAPACITOR : settings.getFloat(FLUX_PER_CAPACITOR_SETTING);
    }

    private final EffectBacking backing;

    FluxSkillEffect() {
        this(null);
    }

    FluxSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }

    private static final class StatNames {
        static final String FLUX_CAPACITY = "stat.fluxCapacity";
        static final String FLUX_DISSIPATION = "stat.fluxDissipation";

        private StatNames() {
        }
    }
}
