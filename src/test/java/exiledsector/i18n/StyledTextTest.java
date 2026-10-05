package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StyledTextTest {

    @Test
    void invertingSwapsGoodAndBadOnly() {
        StyledText text = new StyledText("abcdef", List.of(
                new StyledText.Span(0, 1, Style.GOOD),
                new StyledText.Span(1, 2, Style.BAD),
                new StyledText.Span(2, 3, Style.HULLMOD)));

        assertEquals("<bad>a</bad><good>b</good><hullmod>c</hullmod>def", text.inverted().toMarkup());
    }

    @Test
    void appendingAndJoiningShiftTheSpans() {
        StyledText joined = StyledText.join(StyledText.of(", "), List.of(
                StyledText.styled("Heavy Armor", Style.HULLMOD), StyledText.of("x"), StyledText.styled("Flux", Style.NODE)));

        assertEquals("<hullmod>Heavy Armor</hullmod>, x, <node>Flux</node>", joined.toMarkup());
        assertEquals("<good>a</good>b", StyledText.styled("a", Style.GOOD).append("b").toMarkup());
    }

    @Test
    void insertingTextShiftsTheSpansAfterItAndLeavesTheOnesBeforeIt() {
        StyledText text = StyledText.parse("<good>10%</good> more <bad>hull</bad>.");

        assertEquals("<good>10%</good> (8%-12%) more <bad>hull</bad>.", text.insert(3, " (8%-12%)").toMarkup());
    }

    @Test
    void rejectsOverlappingOrOutOfRangeSpans() {
        assertThrows(IllegalArgumentException.class, () -> new StyledText("abc", List.of(
                new StyledText.Span(0, 2, Style.GOOD), new StyledText.Span(1, 3, Style.BAD))));
        assertThrows(IllegalArgumentException.class, () -> new StyledText("abc", List.of(new StyledText.Span(0, 4, Style.GOOD))));
        assertThrows(IllegalArgumentException.class, () -> new StyledText("abc", List.of(new StyledText.Span(1, 1, Style.GOOD))));
    }
}
