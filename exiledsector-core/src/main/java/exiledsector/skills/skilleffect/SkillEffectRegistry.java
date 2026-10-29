package exiledsector.skills.skilleffect;

import exiledsector.skills.skilleffect.aura.AuraSkillEffect;
import exiledsector.skills.skilleffect.battle.BattleSkillEffect;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.flux.FluxSkillEffect;
import exiledsector.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledsector.skills.skilleffect.movement.MovementSkillEffect;
import exiledsector.skills.skilleffect.onhit.OnHitSkillEffect;
import exiledsector.skills.skilleffect.phase.PhaseSkillEffect;
import exiledsector.skills.skilleffect.readiness.ReadinessSkillEffect;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.skills.skilleffect.shipsystem.ShipSystemSkillEffect;
import exiledsector.skills.skilleffect.special.SlotItemEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.skills.skilleffect.hullmodule.StationModuleSkillEffect;
import exiledsector.skills.skilleffect.weapon.ScopedWeaponEffect;
import exiledsector.skills.skilleffect.weapon.WeaponSkillEffect;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class SkillEffectRegistry {

    private static final Map<String, SkillEffect> BY_NAME = build();

    private SkillEffectRegistry() {
    }

    static SkillEffect byName(String name) {
        SkillEffect effect = registered(name);
        if (effect == null) {
            String currentName = EffectAliases.currentName(name);
            effect = currentName == null ? null : registered(currentName);
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
                DefenseSkillEffect.values(),
                ShieldSkillEffect.values(),
                PhaseSkillEffect.values(),
                ShipSystemSkillEffect.values(),
                WeaponSkillEffect.values(),
                OnHitSkillEffect.values(),
                FighterSkillEffect.values(),
                AuraSkillEffect.values(),
                StationModuleSkillEffect.values(),
                BattleSkillEffect.values(),
                ReadinessSkillEffect.values(),
                LogisticsSkillEffect.values(),
                SpecialSkillEffect.values()
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

    private static SkillEffect registered(String name) {
        SkillEffect effect = BY_NAME.get(name);
        return effect != null ? effect : SlotItemEffect.byName(name);
    }

    static boolean contains(String name) {
        return registered(name) != null;
    }

    static Set<String> names() {
        Set<String> names = new HashSet<>(BY_NAME.keySet());
        SlotItemEffect.all().forEach(effect -> names.add(effect.name()));
        return Collections.unmodifiableSet(names);
    }
}
