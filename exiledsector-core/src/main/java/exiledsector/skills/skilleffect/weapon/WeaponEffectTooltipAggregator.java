package exiledsector.skills.skilleffect.weapon;

import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.StatMode;

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
        return joinUnrelatedScopes(result);
    }

    private static List<SkillTypeEffect> joinUnrelatedScopes(List<SkillTypeEffect> effects) {
        List<SkillTypeEffect> result = new ArrayList<>();
        List<SkillTypeEffect> remaining = new ArrayList<>(effects);
        while (!remaining.isEmpty()) {
            SkillTypeEffect first = remaining.remove(0);
            if (!(first.effect() instanceof ScopedWeaponEffect firstScoped) || !firstScoped.family().qualifiedByScope()) {
                result.add(first);
                continue;
            }
            List<ScopedWeaponEffect> joined = new ArrayList<>(List.of(firstScoped));
            remaining.removeIf(other -> joins(joined, first.magnitude(), other));
            result.add(joined.size() == 1 ? first : new SkillTypeEffect(new JoinedScopeWeaponEffect(joined), first.magnitude()));
        }
        return result;
    }

    private static boolean joins(List<ScopedWeaponEffect> joined, float magnitude, SkillTypeEffect candidate) {
        if (!(candidate.effect() instanceof ScopedWeaponEffect scoped) || candidate.magnitude() != magnitude) {
            return false;
        }
        ScopedWeaponEffect first = joined.get(0);
        boolean unrelated = joined.stream().noneMatch(member -> member.scope() == scoped.scope()
                || member.scope().isAncestorOf(scoped.scope()) || scoped.scope().isAncestorOf(member.scope()));
        if (scoped.family() != first.family() || scoped.mode() != first.mode() || !unrelated) {
            return false;
        }
        joined.add(scoped);
        return true;
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
