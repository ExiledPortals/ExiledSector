package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextWrapperTest {

    private static final float FONT_SIZE = 20f;
    private static final TextWrapper.Metrics ONE_PER_CHAR = text -> text.codePointCount(0, text.length());
    private static final TextWrapper.Metrics VARIABLE = text -> {
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            width += c == ' ' ? 0.5f : Character.isUpperCase(c) ? 1.5f : Character.isDigit(c) ? 1.1f : 1f;
        }
        return width;
    };

    private static StyledText wrap(String markup, float maxWidth) {
        return TextWrapper.wrap(StyledText.parse(markup), ONE_PER_CHAR, FONT_SIZE, maxWidth, 10_000f);
    }

    @Test
    void wrapsEnglishExactlyLikeLazyFont() {
        Random random = new Random(7);
        String[] words = {"Increases", "flux", "capacity", "by", "15%.", "a", "Supercalifragilisticexpialidocious",
                "", "\n", "\n\n", "  ", "Decreases", "10", "more", "ship's", "-", "x"};
        for (int sample = 0; sample < 3000; sample++) {
            StringBuilder text = new StringBuilder();
            int count = 1 + random.nextInt(30);
            for (int i = 0; i < count; i++) {
                text.append(words[random.nextInt(words.length)]);
                if (random.nextInt(4) > 0) {
                    text.append(' ');
                }
            }
            float maxWidth = 3f + random.nextInt(40);
            float maxHeight = random.nextInt(5) == 0 ? FONT_SIZE * (1 + random.nextInt(4)) : 10_000f;
            TextWrapper.Metrics metrics = random.nextBoolean() ? ONE_PER_CHAR : VARIABLE;
            String expected = linesWithin(LazyFontWrapOracle.wrapString(text.toString(), metrics, FONT_SIZE, maxWidth, maxHeight),
                    (int) (maxHeight / FONT_SIZE));
            String actual = TextWrapper.wrap(StyledText.of(text.toString()), metrics, FONT_SIZE, maxWidth, maxHeight).plain();
            assertEquals(expected, actual, "text \"" + text + "\" width " + maxWidth + " height " + maxHeight);
        }
    }

    private static String linesWithin(String wrapped, int maxLines) {
        String[] lines = wrapped.split("\n", -1);
        return lines.length <= maxLines ? wrapped : String.join("\n", Arrays.copyOf(lines, maxLines));
    }

    @Test
    void breaksChineseBetweenCharactersWithoutHyphens() {
        StyledText wrapped = wrap("护盾容量提高十五个百分点并且额外获得更多装甲", 8f);

        assertEquals("护盾容量提高十五\n个百分点并且额外\n获得更多装甲", wrapped.plain());
    }

    @Test
    void neverStartsALineWithClosingPunctuationOrEndsOneWithOpeningPunctuation() {
        assertEquals("护盾容量提\n高，装甲。", wrap("护盾容量提高，装甲。", 6f).plain());
        assertEquals("护盾容量提\n（高）", wrap("护盾容量提（高）", 6f).plain());
    }

    @Test
    void keepsNumbersAndPercentSignsTogether() {
        assertEquals("幅能容量提高\n15%。", wrap("幅能容量提高15%。", 8f).plain());
    }

    @Test
    void highlightsFollowTheirTextAcrossInsertedLineBreaks() {
        StyledText wrapped = wrap("幅能容量提高<good>15%</good>，护盾效率提高<bad>20%</bad>。", 7f);

        assertEquals("幅能容量提高\n<good>15%</good>，护盾效\n率提高<bad>20%</bad>。", wrapped.toMarkup());
    }

    @Test
    void avoidsBreakingInsideAHighlightWhenAnotherBreakExists() {
        StyledText wrapped = wrap("提高<good>护盾效率</good>", 5f);

        assertEquals("提高\n<good>护盾效率</good>", wrapped.toMarkup());
    }

    @Test
    void highlightsFollowTheirTextWhenEnglishBreaksAtSpaces() {
        StyledText wrapped = wrap("Increases flux capacity by <good>15%</good>.", 20f);

        assertEquals("Increases flux\ncapacity by <good>15%</good>.", wrapped.toMarkup());
    }

    @Test
    void mixedTextUsesWhicheverBreakKeepsMoreOnTheLine() {
        assertEquals("使用 Heavy Armor 船\n插", wrap("使用 Heavy Armor 船插", 16f).plain());
    }

    @Test
    void highlightsCutOffByTheLineLimitAreDropped() {
        StyledText wrapped = TextWrapper.wrap(StyledText.parse("aaaa bbbb <good>cccc</good>"), ONE_PER_CHAR, FONT_SIZE, 5f, FONT_SIZE * 2);

        assertEquals("aaaa\nbbbb", wrapped.plain());
        assertTrue(wrapped.spans().isEmpty());
    }

    @Test
    void blankLinesStopAtTheLineLimitLikeEveryOtherLine() {
        assertEquals("a", TextWrapper.wrap(StyledText.of("a\n\n\nb"), ONE_PER_CHAR, 50f, 1).plain());
        assertEquals("a\n", TextWrapper.wrap(StyledText.of("a\n\n\nb"), ONE_PER_CHAR, 50f, 2).plain());
        assertEquals("", TextWrapper.wrap(StyledText.of("\n\n\n"), ONE_PER_CHAR, 50f, 0).plain());
    }

    @Test
    void alwaysMakesProgressWhenNotEvenOneCharacterFits() {
        assertEquals("a-\nb-\nc", TextWrapper.wrap(StyledText.of("abc"), ONE_PER_CHAR, FONT_SIZE, 0.5f, 10_000f).plain());
    }

    @Test
    void recognisesChineseAndFullWidthCharactersAsBreakable() {
        assertTrue(TextWrapper.isCjk("护".codePointAt(0)));
        assertTrue(TextWrapper.isCjk("，".codePointAt(0)));
        assertTrue(TextWrapper.isCjk("。".codePointAt(0)));
        assertFalse(TextWrapper.isCjk('a'));
        assertFalse(TextWrapper.canBreakBefore("ab", 1));
        assertTrue(TextWrapper.canBreakBefore("护盾", 1));
        assertFalse(TextWrapper.canBreakBefore("护，", 1));
        assertEquals(List.of(), TextWrapper.wrap(StyledText.of(""), ONE_PER_CHAR, FONT_SIZE, 5f, 100f).spans());
    }
}
