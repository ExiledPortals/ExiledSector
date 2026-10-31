package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

public record DerivedStatEffect(StatApplier applier, StatMode mode, boolean reappliesAfterShipCreation) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        applier.apply(stats, modId, magnitude);
    }

    @Override
    public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
        if (reappliesAfterShipCreation) {
            applier.apply(ship.getMutableStats(), modId, magnitude);
        }
    }

    @Override
    public boolean appliesAfterOtherEffects() {
        return true;
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.templated(effect, magnitude);
    }
}
