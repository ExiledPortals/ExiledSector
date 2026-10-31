package exiledsector.charts.atlas;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;

public record AtlasLine(String text, Tone tone) {

    public enum Tone { TITLE, PLAIN, MARKER, GOOD, BAD, HINT, STYLED }

    public StyledText styled() {
        return switch (tone) {
            case MARKER -> StyledText.styled(text, Style.HIGHLIGHT);
            case GOOD -> StyledText.styled(text, Style.GOOD);
            case BAD -> StyledText.styled(text, Style.BAD);
            case HINT -> StyledText.styled(text, Style.MUTED);
            case STYLED -> StyledText.parse(text);
            default -> StyledText.of(text);
        };
    }

    public String plain() {
        return tone == Tone.STYLED ? StyledText.parse(text).plain() : text;
    }
}
