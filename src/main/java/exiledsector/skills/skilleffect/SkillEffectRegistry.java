package exiledsector.skills.skilleffect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class SkillEffectRegistry {

    private static final Map<String, SkillEffect> BY_NAME = build();

    private SkillEffectRegistry() {
    }

    static SkillEffect byName(String name) {
        SkillEffect effect = BY_NAME.get(name);
        if (effect == null) {
            String currentName = EffectAliases.currentName(name);
            effect = currentName == null ? null : BY_NAME.get(currentName);
        }
        if (effect == null) {
            throw new IllegalArgumentException("Unknown SkillEffect: " + name);
        }
        return effect;
    }

    private static Map<String, SkillEffect> build() {
        Map<String, SkillEffect> registry = new HashMap<>();
        SkillEffect[][] groups = {
                MovementSkillEffect.values(),
                FluxSkillEffect.values(),
                WeaponSkillEffect.values(),
                ShieldSkillEffect.values(),
                LogisticsSkillEffect.values(),
                FighterSkillEffect.values(),
                PhaseSkillEffect.values(),
                MiscSkillEffect.values(),
                DefenseSkillEffect.values(),
                CombatSkillEffect.values(),
                CompatSkillEffect.values()
        };
        for (SkillEffect[] group : groups) {
            for (SkillEffect effect : group) {
                register(registry, effect);
            }
        }
        for (SkillEffect effect : ScopedWeaponEffect.all()) {
            register(registry, effect);
        }
        return registry;
    }

    private static void register(Map<String, SkillEffect> registry, SkillEffect effect) {
        SkillEffect existing = registry.put(effect.name(), effect);
        if (existing != null) {
            throw new IllegalStateException("Duplicate SkillEffect name: " + effect.name());
        }
    }

    static boolean contains(String name) {
        return BY_NAME.containsKey(name);
    }

    static Set<String> names() {
        return Collections.unmodifiableSet(BY_NAME.keySet());
    }
}
