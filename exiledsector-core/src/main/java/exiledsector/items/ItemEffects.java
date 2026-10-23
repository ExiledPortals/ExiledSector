package exiledsector.items;

import exiledlib.modifiers.ModifierEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.modifiers.ModifierEffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ItemEffects {

    public static final ModifierEffects SKILL_EFFECTS = new SkillModifierEffects();
    private static final AtomicReference<Registry> REGISTRY = new AtomicReference<>(Registry.of(builtInResolvers()));

    private record Registry(Map<ItemType, ModifierEffects> byType, List<ModifierEffects> distinct) {

        static Registry of(Map<ItemType, ModifierEffects> byType) {
            List<ModifierEffects> distinct = new ArrayList<>();
            byType.values().forEach(effects -> {
                if (distinct.stream().noneMatch(known -> known == effects)) {
                    distinct.add(effects);
                }
            });
            return new Registry(Collections.unmodifiableMap(new EnumMap<>(byType)), List.copyOf(distinct));
        }
    }

    private ItemEffects() {
    }

    public static ModifierEffects forType(ItemType type) {
        ModifierEffects effects = type == null ? null : REGISTRY.get().byType().get(type);
        return effects == null ? SKILL_EFFECTS : effects;
    }

    public static ModifierEffects forTypes(Collection<ItemType> types) {
        ModifierEffects shared = null;
        for (ItemType type : types) {
            ModifierEffects effects = forType(type);
            if (shared != null && shared != effects) {
                throw new IllegalArgumentException("item types " + types.stream().map(ItemType::id).toList() + " describe their effects differently, so they cannot share a modifier");
            }
            shared = effects;
        }
        return shared == null ? SKILL_EFFECTS : shared;
    }

    public static ModifierEffects register(ItemType type, ModifierEffects effects) {
        if (type == null || effects == null) {
            throw new IllegalArgumentException("an effect resolver needs an item type and a resolver");
        }
        Registry previous;
        Registry updated;
        do {
            previous = REGISTRY.get();
            Map<ItemType, ModifierEffects> byType = new EnumMap<>(ItemType.class);
            byType.putAll(previous.byType());
            byType.put(type, effects);
            updated = Registry.of(byType);
        } while (!REGISTRY.compareAndSet(previous, updated));
        return previous.byType().get(type);
    }

    static String anyCurrentName(String effectName) {
        if (effectName == null) {
            return null;
        }
        for (ModifierEffects effects : REGISTRY.get().distinct()) {
            String currentName = effects.currentName(effectName);
            if (currentName != null) {
                return currentName;
            }
        }
        return null;
    }

    private static Map<ItemType, ModifierEffects> builtInResolvers() {
        Map<ItemType, ModifierEffects> byType = new EnumMap<>(ItemType.class);
        byType.put(ItemType.SUBROUTINE, SKILL_EFFECTS);
        byType.put(ItemType.HULL_ITEM, SKILL_EFFECTS);
        return byType;
    }

    private static final class SkillModifierEffects implements ModifierEffects {

        @Override
        public ModifierEffect byName(String effectName) {
            if (effectName == null) {
                return null;
            }
            try {
                return SkillEffect.byName(effectName);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
