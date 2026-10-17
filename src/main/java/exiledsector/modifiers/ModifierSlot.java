package exiledsector.modifiers;

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
}
