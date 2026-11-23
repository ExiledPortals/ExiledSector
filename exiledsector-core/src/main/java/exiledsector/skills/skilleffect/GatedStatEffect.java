package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.i18n.StyledText;

import java.util.function.Predicate;

public record GatedStatEffect(StatMode mode, StatTarget target, boolean lowerIsBetter, Predicate<MutableShipStatsAPI> gate)
        implements EffectBacking {

    private static final String PHASE_FIELD_HULL_MOD = "phasefield";

    public static GatedStatEffect withoutCaptain(StatMode mode, StatTarget target, boolean lowerIsBetter) {
        return new GatedStatEffect(mode, target, lowerIsBetter, ShipCaptain::absent);
    }

    public static GatedStatEffect withoutPhaseCloak(StatMode mode, StatTarget target, boolean lowerIsBetter) {
        return new GatedStatEffect(mode, target, lowerIsBetter, stats -> !hasPhaseCloak(stats));
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        target.apply(stats, modId, mode, gate.test(stats) ? magnitude : 0f);
    }

    @Override
    public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
        target.applyAfterShipCreation(ship);
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.signed(effect, magnitude).styled();
    }

    static boolean hasPhaseCloak(MutableShipStatsAPI stats) {
        ShipVariantAPI variant = stats.getVariant();
        if (variant == null || variant.getHullSpec() == null) {
            return false;
        }
        return variant.getHullSpec().isPhase() || variant.hasHullMod(PHASE_FIELD_HULL_MOD);
    }
}
