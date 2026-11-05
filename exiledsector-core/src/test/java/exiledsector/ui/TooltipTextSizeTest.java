package exiledsector.ui;

import exiledsector.items.ItemTooltip;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipTextSizeTest {

    @AfterEach
    void restoreDefault() {
        TooltipTextSize.setLargeForTests(TooltipTextSize.DEFAULT_LARGE);
    }

    @Test
    void tooltipsDefaultToTheVanillaSizes() {
        TooltipText vanilla = SkillTreePanelStyle.tooltipText();

        assertEquals(15f, vanilla.body().size());
        assertEquals(360f, vanilla.maxTextWidth());
        assertEquals(450f, ItemTooltip.width());
    }

    @Test
    void largeTooltipTextRestoresTheEarlierSizes() {
        TooltipTextSize.setLargeForTests(true);
        TooltipText large = SkillTreePanelStyle.tooltipText();

        assertEquals(24f, large.title().size());
        assertEquals(20f, large.body().size());
        assertEquals(17f, large.flavour().size());
        assertEquals(480f, large.maxTextWidth());
        assertEquals(700f, ItemTooltip.width());
    }

    @Test
    void largeTooltipTextGrowsEveryTableMeasure() {
        TooltipText.Table vanilla = SkillTreePanelStyle.tooltipText().table();
        TooltipTextSize.setLargeForTests(true);
        TooltipText.Table large = SkillTreePanelStyle.tooltipText().table();

        assertTrue(large.cell().size() > vanilla.cell().size());
        assertTrue(large.heading().size() > vanilla.heading().size());
        assertTrue(large.rowHeight() > vanilla.rowHeight());
        assertTrue(large.headingHeight() > vanilla.headingHeight());
    }
}
