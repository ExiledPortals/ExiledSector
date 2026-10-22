package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledlib.i18n.StyledText;

record PerDModMultEffect(StatTarget target, String statKey, boolean lowerIsBetter) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        float mult = SkillEffectSupport.compoundMultPerDMod(stats, magnitude);
        target.apply(stats, modId, StatMode.MULT, (mult - 1f) * 100f);
    }

    @Override
    public boolean isMultiplicative() {
        return true;
    }

    @Override
    public boolean supportsTemporaryGating() {
        return target.supportsTemporaryGating();
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return StatMode.MULT.describeStat(magnitude, statKey);
    }
}
