package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;

public record PerDModPercentEffect(StatTarget target, String statKey, boolean lowerIsBetter) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        target.apply(stats, modId, StatMode.PERCENT, magnitude * SkillEffectSupport.countedDMods(stats.getVariant()));
    }

    @Override
    public boolean supportsTemporaryGating() {
        return target.supportsTemporaryGating();
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return StatMode.PERCENT.describeStat(magnitude, statKey);
    }
}
