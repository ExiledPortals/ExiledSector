package exiledsector.skills;

public final class TypeAffinity {

    private TypeAffinity() {
    }

    public static SkillTypeEffect of(SkillType type) {
        if (type == null) {
            return null;
        }
        for (SkillTypeEffect effect : type.getEffects()) {
            if (AffinitySteps.isAffinity(effect.effect())) {
                return effect;
            }
        }
        return null;
    }

    static boolean same(SkillType first, SkillType second) {
        SkillTypeEffect firstAffinity = of(first);
        SkillTypeEffect secondAffinity = of(second);
        return firstAffinity != null && secondAffinity != null && firstAffinity.effect() == secondAffinity.effect();
    }
}
