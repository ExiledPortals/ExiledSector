package exiledsector.ui.util;

import exiledsector.i18n.StyledText;
import exiledsector.skills.DescriptionLine;
import exiledsector.ui.SkillTreePanelStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.lazylib.ui.LazyFont;
import org.mockito.InOrder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TextLabelTest {

    private static final Color GREEN = SkillTreePanelStyle.POSITIVE_STAT_COLOR;
    private static final Color RED = SkillTreePanelStyle.NEGATIVE_STAT_COLOR;
    private static final float FONT_SIZE = 20f;

    private final LazyFont font = mock(LazyFont.class);
    private final LazyFont.DrawableString drawable = mock(LazyFont.DrawableString.class);

    @BeforeEach
    void stubFont() {
        when(font.createText(anyString(), any(Color.class), anyFloat())).thenReturn(drawable);
        when(font.calcWidth(anyString(), anyFloat())).thenAnswer(invocation -> invocation.<String>getArgument(0).length() * 10f);
    }

    private TextLabel label(Color color) {
        return new TextLabel(() -> font, FONT_SIZE, color, LazyFont.TextAnchor.TOP_LEFT);
    }

    private static DescriptionLine line(String markup, boolean lowerIsBetter) {
        return new DescriptionLine(StyledText.parse(markup), lowerIsBetter);
    }

    private void drawDescription(DescriptionLine... paragraphs) {
        List<StyledText> displayed = new ArrayList<>();
        for (DescriptionLine paragraph : paragraphs) {
            displayed.add(paragraph.display());
        }
        label(Color.WHITE).setParagraphs(displayed, 10_000f, 800f, SkillTreePanelStyle::standardHighlightColor).draw(0f, 0f);
    }

    @Test
    void colourChangesArePlacedOneCharacterLateToCompensateForLazyFontApplyingThemEarly() {
        drawDescription(line("Increases flux capacity by <good>15%</good>.", false));

        InOrder order = inOrder(drawable);
        order.verify(drawable).setText("");
        order.verify(drawable).append("Increases flux capacity by 1");
        order.verify(drawable).append("5%.", GREEN);
        order.verify(drawable).append(" ");
    }

    @Test
    void aHighlightAtTheStartOfALaterParagraphIsStillColoured() {
        drawDescription(line("Intro.", false), line("<good>100%</good> more flux dissipation.", false));

        InOrder order = inOrder(drawable);
        order.verify(drawable).append("Intro.\n\n1");
        order.verify(drawable).append("00% ", GREEN);
        order.verify(drawable).append("more flux dissipation. ");
    }

    @Test
    void aHighlightEndingBeforeALineBreakResetsOnTheNextDrawnCharacter() {
        drawDescription(line("Increases armor by <good>10%</good>", false), line("Next.", false));

        InOrder order = inOrder(drawable);
        order.verify(drawable).append("Increases armor by 1");
        order.verify(drawable).append("0%\n\nN", GREEN);
        order.verify(drawable).append("ext. ");
    }

    @Test
    void lowerIsBetterLinesSwapGreenAndRed() {
        drawDescription(line("Decreases shield upkeep by <bad>20%</bad>.", true));

        verify(drawable).append("0%.", GREEN);
    }

    @Test
    void lowerIsBetterTurnsAMoreIncreaseRed() {
        drawDescription(line("<good>25%</good> more weapon recoil.", true));

        InOrder order = inOrder(drawable);
        order.verify(drawable).append("2");
        order.verify(drawable).append("5% ", RED);
    }

    @Test
    void plainTextFadesThroughTheBaseColourWithoutRebuildingTheText() {
        TextLabel label = label(Color.WHITE).set("Hello");
        label.draw(0f, 0f);
        clearInvocations(drawable);

        label.setAlpha(0.5f).draw(0f, 0f);

        verify(drawable).setBaseColor(new Color(255, 255, 255, 128));
        verify(drawable, never()).setText(anyString());
    }

    @Test
    void highlightedTextOnlyRebuildsWhenTheFadeCrossesAQuantisedStep() {
        TextLabel label = label(Color.WHITE).setStyled(StyledText.parse("Up <good>5%</good>"), SkillTreePanelStyle::standardHighlightColor);
        label.draw(0f, 0f);
        clearInvocations(drawable);

        label.setAlpha(0.999f).draw(0f, 0f);
        verify(drawable, never()).setText(anyString());

        label.setAlpha(0.5f).draw(0f, 0f);
        verify(drawable, times(1)).setText("");
        int fadedAlpha = TextLabel.quantisedAlpha(TextLabel.toByteAlpha(0.5f));
        verify(drawable).append("% ", new Color(GREEN.getRed(), GREEN.getGreen(), GREEN.getBlue(), fadedAlpha));
    }

    @Test
    void quantisedAlphaKeepsTheEndsExactAndSnapsTheMiddle() {
        assertEquals(0, TextLabel.quantisedAlpha(0));
        assertEquals(255, TextLabel.quantisedAlpha(255));
        assertEquals(TextLabel.quantisedAlpha(127), TextLabel.quantisedAlpha(128));
        assertEquals(TextLabel.HIGHLIGHT_ALPHA_STEPS + 1, java.util.stream.IntStream.rangeClosed(0, 255)
                .map(TextLabel::quantisedAlpha).distinct().count());
    }

    @Test
    void measurementCoversEveryLineWithoutTouchingTheGlBuffer() {
        TextLabel label = label(Color.WHITE).set("abc\nabcdef\n");

        assertEquals(60f, label.width());
        assertEquals(3 * FONT_SIZE, label.height());
        verify(font, never()).createText(anyString(), any(Color.class), anyFloat());
    }

    @Test
    void refreshRunsTheWriterOnlyWhenTheSignatureChanges() {
        TextLabel label = label(Color.WHITE);
        List<String> writes = new ArrayList<>();

        label.refresh("a", written -> writes.add("a"));
        label.refresh("a", written -> writes.add("a again"));
        label.refresh("b", written -> writes.add("b"));

        assertEquals(List.of("a", "b"), writes);
    }

    @Test
    void oneDrawableIsReusedAcrossTextChanges() {
        TextLabel label = label(Color.WHITE).set("first");
        label.draw(0f, 0f);
        label.set("second").draw(0f, 0f);

        verify(font, times(1)).createText(anyString(), any(Color.class), anyFloat());
        verify(drawable).setText("second");
    }
}
