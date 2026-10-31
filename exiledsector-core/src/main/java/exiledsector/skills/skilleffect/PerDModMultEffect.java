package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;

public record PerDModMultEffect(StatApplier multApplier, String statKey, boolean lowerIsBetter, boolean supportsTemporaryGating)
        implements EffectBacking {

    public PerDModMultEffect(StatTarget target, String statKey, boolean lowerIsBetter) {
        this((stats, modId, multPercent) -> target.apply(stats, modId, StatMode.MULT, multPercent), statKey, lowerIsBetter,
                target.supportsTemporaryGating());
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        float mult = SkillEffectSupport.compoundMultPerDMod(stats, magnitude);
        multApplier.apply(stats, modId, (mult - 1f) * 100f);
    }

    @Override
    public boolean isMultiplicative() {
        return true;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return StatMode.MULT.describeStat(magnitude, statKey);
    }
}
