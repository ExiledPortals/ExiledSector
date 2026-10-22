package exiledsector.modifiers;

import exiledlib.modifiers.ModifierEffect;

public interface ModifierEffects {

    ModifierEffect byName(String effectName);

    default boolean canRoll(String effectName) {
        return true;
    }

    default String currentName(String listedName) {
        ModifierEffect effect = listedName == null ? null : byName(listedName);
        return effect == null ? null : effect.name();
    }
}
