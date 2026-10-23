package exiledlib.skills.skilleffect;

import exiledlib.skills.skilleffect.aura.AuraSkillEffect;
import exiledlib.skills.skilleffect.battle.BattleSkillEffect;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import exiledlib.skills.skilleffect.fighter.FighterSkillEffect;
import exiledlib.skills.skilleffect.flux.FluxSkillEffect;
import exiledlib.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledlib.skills.skilleffect.movement.MovementSkillEffect;
import exiledlib.skills.skilleffect.onhit.OnHitSkillEffect;
import exiledlib.skills.skilleffect.phase.PhaseSkillEffect;
import exiledlib.skills.skilleffect.readiness.ReadinessSkillEffect;
import exiledlib.skills.skilleffect.shield.ShieldSkillEffect;
import exiledlib.skills.skilleffect.shipsystem.ShipSystemSkillEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;
import exiledlib.skills.skilleffect.stationmodule.StationModuleSkillEffect;
import exiledlib.skills.skilleffect.weapon.ScopedWeaponEffect;
import exiledlib.skills.skilleffect.weapon.WeaponSkillEffect;

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

    static boolean contains(String name) {
        return BY_NAME.containsKey(name);
    }

    static Set<String> names() {
        return Collections.unmodifiableSet(BY_NAME.keySet());
    }
}
