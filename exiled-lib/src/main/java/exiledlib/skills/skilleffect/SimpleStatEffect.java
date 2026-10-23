package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledlib.i18n.StyledText;

public record SimpleStatEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        target.apply(stats, modId, mode, magnitude);
    }

    StyledText description(float magnitude) {
        return mode.describeStat(magnitude, statKey);
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return description(magnitude);
    }

    @Override
    public boolean supportsTemporaryGating() {
        return target.supportsTemporaryGating();
    }
}
