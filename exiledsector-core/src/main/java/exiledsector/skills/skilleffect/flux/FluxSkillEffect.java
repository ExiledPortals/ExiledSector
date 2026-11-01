package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.compat.LostSectorCompat;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.EffectBacking;

import static exiledsector.skills.skilleffect.Backings.listener;
import static exiledsector.skills.skilleffect.Backings.statEffect;
import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum FluxSkillEffect implements BackedSkillEffect {

    FLUX_CAPACITY_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false)),
    FLUX_CAPACITY_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false)),
    FLUX_CAPACITY_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false)),
    FLUX_DISSIPATION_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false)),
    FLUX_DISSIPATION_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false)),
    FLUX_DISSIPATION_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false)),
    VENT_RATE_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeed", false)),
    VENT_RATE_MULT(statEffect(MULT, stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeed", false)),
    ZERO_FLUX_SPEED_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getZeroFluxSpeedBoost), "stat.zeroFluxSpeedBoost", false)),
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
    FLUX_SCALED_TOP_SPEED_FLAT(listener(FluxScaledVolatilityListener.TOP_SPEED_KEY,
            FluxScaledVolatilityListener.class, FluxScaledVolatilityListener::new)),
    FLUX_SCALED_RATE_OF_FIRE_PERCENT(listener(FluxScaledVolatilityListener.RATE_OF_FIRE_KEY,
            FluxScaledVolatilityListener.class, FluxScaledVolatilityListener::new)),
    AUGMENTED_FLUX_SCALED_PENALTY_REDUCTION_PERCENT(listener(FluxScaledVolatilityListener.AUGMENTED_PENALTY_REDUCTION_KEY,
            FluxScaledVolatilityListener.class, FluxScaledVolatilityListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return LostSectorCompat.isModEnabled() ? super.description(magnitude) : null;
        }
    };

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
