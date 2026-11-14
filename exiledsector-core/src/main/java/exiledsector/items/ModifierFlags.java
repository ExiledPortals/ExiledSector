package exiledsector.items;

public record ModifierFlags(boolean immutable, boolean overcharged, boolean warped) {

    public static final ModifierFlags NONE = new ModifierFlags(false, false, false);

    public ModifierFlags asImmutable() {
        return new ModifierFlags(true, overcharged, warped);
    }

    public ModifierFlags asOvercharged() {
        return new ModifierFlags(immutable, true, warped);
    }

    public ModifierFlags withoutOvercharge() {
        return new ModifierFlags(immutable, false, warped);
    }

    public ModifierFlags asWarped() {
        return new ModifierFlags(immutable, overcharged, true);
    }
}
