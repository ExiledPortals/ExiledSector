package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum FluxSkillEffect implements BackedSkillEffect {

    FLUX_CAPACITY_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false),
    FLUX_CAPACITY_FLAT(FLAT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false),
    FLUX_CAPACITY_MULT(MULT, liveStat(MutableShipStatsAPI::getFluxCapacity), StatNames.FLUX_CAPACITY, false),
    FLUX_DISSIPATION_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false),
    FLUX_DISSIPATION_FLAT(FLAT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false),
    FLUX_DISSIPATION_MULT(MULT, liveStat(MutableShipStatsAPI::getFluxDissipation), StatNames.FLUX_DISSIPATION, false),
    VENT_RATE_PERCENT(PERCENT, stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeed", false),
    VENT_RATE_MULT(MULT, stat(MutableShipStatsAPI::getVentRateMult), "stat.ventingSpeed", false),
    ZERO_FLUX_ALWAYS_ON {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getZeroFluxMinimumFluxLevel().modifyFlat(modId, 2f);
        }
    },
    FLUX_DISSIPATION_WHILE_VENTING_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getFluxDissipation),
            "stat.fluxDissipationWhileVenting", ship -> ship.getFluxTracker().isVenting()));

    private final EffectBacking backing;

    FluxSkillEffect() {
        this((EffectBacking) null);
    }

    FluxSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
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
