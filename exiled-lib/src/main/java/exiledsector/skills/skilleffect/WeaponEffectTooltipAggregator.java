package exiledsector.skills.skilleffect;

import exiledsector.skills.SkillTypeEffect;

import java.util.ArrayList;
import java.util.List;

public final class WeaponEffectTooltipAggregator {

    private static final List<WeaponScope> PARENTS_DEEPEST_FIRST = List.of(WeaponScope.ENERGY, WeaponScope.ALL);

    private WeaponEffectTooltipAggregator() {
    }

    public static List<SkillTypeEffect> collapse(List<SkillTypeEffect> effects) {
        List<SkillTypeEffect> result = new ArrayList<>(effects);
        boolean changed = true;
        while (changed) {
            changed = collapseOnce(result);
        }
        return result;
    }

    private static boolean collapseOnce(List<SkillTypeEffect> effects) {
        for (WeaponScope parent : PARENTS_DEEPEST_FIRST) {
            for (SkillTypeEffect candidate : effects) {
                if (candidate.effect() instanceof ScopedWeaponEffect scoped && scoped.scope().parent() == parent
                        && replaceChildrenWithParent(effects, scoped, candidate.magnitude(), parent)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean replaceChildrenWithParent(List<SkillTypeEffect> effects, ScopedWeaponEffect sibling,
                                                     float magnitude, WeaponScope parent) {
        ScopedWeaponEffect parentEffect = ScopedWeaponEffect.find(sibling.family(), parent, sibling.mode());
        if (parentEffect == null) {
            return false;
        }
        List<Integer> childIndices = new ArrayList<>();
        for (WeaponScope child : parent.children()) {
            int index = indexOf(effects, sibling.family(), child, sibling.mode(), magnitude);
            if (index < 0) {
                return false;
            }
            childIndices.add(index);
        }
        int insertAt = childIndices.stream().mapToInt(Integer::intValue).min().orElse(0);
        childIndices.sort((a, b) -> Integer.compare(b, a));
        for (int index : childIndices) {
            effects.remove(index);
        }
        effects.add(insertAt, new SkillTypeEffect(parentEffect, magnitude));
        return true;
    }

    private static int indexOf(List<SkillTypeEffect> effects, WeaponStatFamily family, WeaponScope scope,
                               StatMode mode, float magnitude) {
        for (int i = 0; i < effects.size(); i++) {
            SkillTypeEffect entry = effects.get(i);
            boolean matches = entry.effect() instanceof ScopedWeaponEffect scoped && scoped.family() == family
                    && scoped.scope() == scope && scoped.mode() == mode && entry.magnitude() == magnitude;
            if (matches) {
                return i;
            }
        }
        return -1;
    }
}
