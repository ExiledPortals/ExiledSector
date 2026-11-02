package exiledsector.modifiers;

import exiledsector.i18n.StyledText;

import java.util.List;

public interface ModifierEffect {

    String name();

    StyledText description(float magnitude);

    default boolean invertsDescriptionColours() {
        return false;
    }

    default String detailTitle() {
        return null;
    }

    default List<StyledText> detailLines() {
        return List.of();
    }
}
