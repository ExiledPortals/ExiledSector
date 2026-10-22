package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateTest {

    private static StyledText render(String raw, Map<String, StyledText> args) {
        return Template.parse(raw).render(args);
    }

    @Test
    void fillsNamedPlaceholdersAndTurnsTagsIntoSpans() {
        StyledText text = render("Increases {stat} by <good>{value}%</good>.",
                Map.of("stat", StyledText.of("armor"), "value", StyledText.of("10")));

        assertEquals("Increases armor by 10%.", text.plain());
        assertEquals(List.of(new StyledText.Span(19, 22, Style.GOOD)), text.spans());
    }

    @Test
    void theSameTemplateHighlightsTheRightWordsInAnotherWordOrder() {
        StyledText text = render("{stat}提高<good>{value}%</good>。",
                Map.of("stat", StyledText.of("装甲"), "value", StyledText.of("10")));

        assertEquals("装甲提高10%。", text.plain());
        assertEquals("装甲提高<good>10%</good>。", text.toMarkup());
    }

    @Test
    void argumentsAreInsertedVerbatimEvenWhenTheyLookLikeMarkup() {
        StyledText text = render("Name: {name}", Map.of("name", StyledText.of("<good>{value}</good> 100%")));

        assertEquals("Name: <good>{value}</good> 100%", text.plain());
        assertTrue(text.spans().isEmpty());
    }

    @Test
    void styledArgumentsKeepTheirSpansOutsideTags() {
        StyledText names = StyledText.styled("Heavy Armor", Style.HULLMOD);
        StyledText text = render("With {names}.", Map.of("names", names));

        assertEquals("With <hullmod>Heavy Armor</hullmod>.", text.toMarkup());
    }

    @Test
    void anOuterTagWinsOverSpansInsideAnArgument() {
        StyledText inner = StyledText.styled("10", Style.BAD);
        StyledText text = render("<good>{value}%</good>", Map.of("value", inner));

        assertEquals("<good>10%</good>", text.toMarkup());
    }

    @Test
    void missingArgumentsStayVisibleAsPlaceholders() {
        assertEquals("Hello {name}", render("Hello {name}", Map.of()).plain());
    }

    @Test
    void unknownTagsAndBracesThatAreNotPlaceholdersAreLiteralText() {
        StyledText text = render("<b>bold</b> {not a placeholder} {1} a < b > c", Map.of());

        assertEquals("<b>bold</b> {not a placeholder} {1} a < b > c", text.plain());
        assertTrue(text.spans().isEmpty());
    }

    @Test
    void anUnclosedTagRunsToTheEndAndAStrayCloseIsIgnored() {
        assertEquals("a<good>bc</good>", render("a<good>bc", Map.of()).toMarkup());
        assertEquals("ab", render("a</good>b", Map.of()).toMarkup());
    }

    @Test
    void emptyTagsProduceNoSpan() {
        assertTrue(render("a<hl></hl>b", Map.of()).spans().isEmpty());
    }

    @Test
    void reportsPlaceholdersTagsAndBalance() {
        Template template = Template.parse("{a} <good>{b}</good> <node>x</node> {a}");

        assertEquals(Set.of("a", "b"), template.placeholders());
        assertEquals(List.of("good", "/good", "node", "/node"), template.tagSequence());
        assertTrue(template.hasBalancedTags());
        assertFalse(Template.parse("<good>a").hasBalancedTags());
        assertFalse(Template.parse("<good><bad>a</bad></good>").hasBalancedTags());
        assertFalse(Template.parse("<good>a</bad>").hasBalancedTags());
    }
}
