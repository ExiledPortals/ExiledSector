package exiledsector.ui;

import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VanillaTextTest {

    private static final Function<Style, Color> PALETTE = style -> style == Style.GOOD ? Color.GREEN : Color.YELLOW;

    @Test
    void englishTextIsPassedThroughUnchangedWithHighlightsInOrder() {
        VanillaText.Prepared prepared = VanillaText.prepare(
                StyledText.parse("Increases armor by <good>10%</good> and hull by <hl>10%</hl>."), PALETTE);

        assertEquals("Increases armor by 10% and hull by 10%.", prepared.text());
        assertArrayEquals(new String[]{"10%", "10%"}, prepared.highlights());
        assertArrayEquals(new Color[]{Color.GREEN, Color.YELLOW}, prepared.colors());
    }

    @Test
    void chineseHighlightsGetTheSpacesStarsectorNeedsToFindThem() {
        VanillaText.Prepared prepared = VanillaText.prepare(StyledText.parse("装甲提高<good>10%</good>。"), PALETTE);

        assertEquals("装甲提高 10% 。", prepared.text());
        assertArrayEquals(new String[]{"10%"}, prepared.highlights());
    }

    @Test
    void highlightsAtTheEdgesOrNextToAsciiPunctuationNeedNoPadding() {
        assertEquals("10%: 装甲", VanillaText.prepare(StyledText.parse("<good>10%</good>: 装甲"), PALETTE).text());
        assertEquals("(装甲)", VanillaText.prepare(StyledText.parse("(<hl>装甲</hl>)"), PALETTE).text());
    }

    @Test
    void aParagraphWithHighlightsKeepsItsBaseColourForTheRestOfTheText() {
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class);
        LabelAPI label = mock(LabelAPI.class);
        when(tooltip.addPara(anyString(), anyFloat(), (Color) any(), (Color) any(), any(String[].class))).thenReturn(label);

        VanillaText.addPara(tooltip, StyledText.parse("Increases armor by <good>10%</good>."), 3f, Color.GRAY, PALETTE);

        verify(tooltip).addPara("%s", 3f, Color.GRAY, Color.GRAY, "Increases armor by 10%.");
        verify(label).setHighlight("10%");
        verify(label).setHighlightColors(Color.GREEN);
    }

    @Test
    void textIsAlwaysAnArgumentSoPercentSignsAreNeverFormatted() {
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class);
        LabelAPI label = mock(LabelAPI.class);
        when(tooltip.addPara(anyString(), anyFloat(), (Color) any(), (Color) any(), any(String[].class))).thenReturn(label);

        VanillaText.addPara(tooltip, StyledText.of("100% plain"), 3f, Color.WHITE, PALETTE);

        verify(tooltip).addPara("%s", 3f, Color.WHITE, Color.WHITE, "100% plain");
        verify(label, never()).setHighlight(any(String[].class));
    }
}
