package exiledsector.skills;

import exiledsector.i18n.StyledText;

public record DescriptionLine(StyledText text, boolean lowerIsBetter) {

    public StyledText display() {
        return lowerIsBetter ? text.inverted() : text;
    }

    public String plain() {
        return text.plain();
    }
}
