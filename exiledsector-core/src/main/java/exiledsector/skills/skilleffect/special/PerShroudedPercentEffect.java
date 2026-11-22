package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

public record PerShroudedPercentEffect(StatTarget target, String statKey, boolean lowerIsBetter) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        target.apply(stats, modId, StatMode.PERCENT, magnitude * ShroudedCount.of(stats.getFleetMember(), stats.getVariant()));
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
