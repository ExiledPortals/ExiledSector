package exiledlib.skills;

import java.util.List;

public record EffectContribution(Object origin, String modIdPrefix, List<SkillTypeEffect> effects, Float temporarySeconds,
                                 String vanillaHullModId, boolean countsTowardBonusScale, boolean bonusScaled) {

    public static EffectContribution scaledPermanent(Object origin, String modIdPrefix, List<SkillTypeEffect> effects) {
        return new EffectContribution(origin, modIdPrefix, effects, null, null, true, true);
    }

    public boolean permanent() {
        return temporarySeconds == null;
    }

    EffectContribution withEffects(List<SkillTypeEffect> resolvedEffects) {
        return new EffectContribution(origin, modIdPrefix, resolvedEffects, temporarySeconds, vanillaHullModId, countsTowardBonusScale, bonusScaled);
    }
}
