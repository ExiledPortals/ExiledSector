package exiledsector.skills;

import java.util.List;

public record EffectContribution(Object origin, String modIdPrefix, List<SkillTypeEffect> effects, Float temporarySeconds,
                                 String vanillaHullModId, boolean countsTowardBonusScale, boolean bonusScaled, CombatGate gate) {

    public EffectContribution(Object origin, String modIdPrefix, List<SkillTypeEffect> effects, Float temporarySeconds,
                              String vanillaHullModId, boolean countsTowardBonusScale, boolean bonusScaled) {
        this(origin, modIdPrefix, effects, temporarySeconds, vanillaHullModId, countsTowardBonusScale, bonusScaled, null);
    }

    public static EffectContribution scaledPermanent(Object origin, String modIdPrefix, List<SkillTypeEffect> effects) {
        return new EffectContribution(origin, modIdPrefix, effects, null, null, true, true);
    }

    public boolean permanent() {
        return temporarySeconds == null && gate == null;
    }

    EffectContribution withEffects(List<SkillTypeEffect> resolvedEffects) {
        return new EffectContribution(origin, modIdPrefix, resolvedEffects, temporarySeconds, vanillaHullModId, countsTowardBonusScale, bonusScaled, gate);
    }
}
