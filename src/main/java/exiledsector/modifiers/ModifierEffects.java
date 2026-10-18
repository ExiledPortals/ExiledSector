package exiledsector.modifiers;

public interface ModifierEffects {

    ModifierEffect byName(String effectName);

    default String currentName(String listedName) {
        ModifierEffect effect = listedName == null ? null : byName(listedName);
        return effect == null ? null : effect.name();
    }
}
