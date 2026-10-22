package exiledlib.modifiers;

import exiledlib.i18n.StyledText;

public interface ModifierEffect {

    String name();

    StyledText description(float magnitude);

    default boolean invertsDescriptionColours() {
        return false;
    }
}
