package exiledlib.modifiers;

import java.util.Locale;

public enum ModifierSlot {
    IMPLICIT(ModifierCategory.IMPLICIT),
    PREFIX(ModifierCategory.EXPLICIT),
    SUFFIX(ModifierCategory.EXPLICIT);

    private final ModifierCategory category;

    ModifierSlot(ModifierCategory category) {
        this.category = category;
    }

    public ModifierCategory category() {
        return category;
    }

    public boolean isExplicit() {
        return category == ModifierCategory.EXPLICIT;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ModifierSlot byIdOrNull(String id) {
        for (ModifierSlot slot : values()) {
            if (slot.id().equals(id)) {
                return slot;
            }
        }
        return null;
    }
}
