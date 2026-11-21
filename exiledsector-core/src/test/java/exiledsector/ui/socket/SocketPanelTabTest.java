package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.Fonts;
import exiledsector.i18n.RealCatalogue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SocketPanelTabTest {

    @Test
    void theItemsPanelHasHullItemsThenHullUpgradesThenHullPatterns() {
        assertEquals(List.of(SocketPanelTab.HULL_ITEMS, SocketPanelTab.HULL_UPGRADES, SocketPanelTab.HULL_PATTERNS), List.of(SocketPanelTab.values()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void everyTabLabelIsTranslated(String language) {
        Map<String, String> catalogue = RealCatalogue.entries(language);
        for (SocketPanelTab tab : SocketPanelTab.values()) {
            assertNotNull(catalogue.get(tab.labelKey()), tab + " in " + language);
            assertNotNull(catalogue.get(tab.shortLabelKey()), tab + " short in " + language);
        }
    }

    @Test
    void theTabRowFallsBackToNarrowerFontsThenShortLabelsWhenThreeTabsDoNotFitBesideClose() {
        List<String> fullLabels = List.of("Hull Items", "Hull Upgrades", "Hull Patterns");
        List<String> shortLabels = List.of("Items", "Upgrades", "Patterns");
        BiFunction<String, String, Float> measure = (label, font) -> label.length() * charWidth(font);

        assertEquals(new HoloPanel.TabRowFit(fullLabels, 0), HoloPanel.fittingTabRow(fullLabels, shortLabels, 1000f, measure));
        assertEquals(new HoloPanel.TabRowFit(fullLabels, 1), HoloPanel.fittingTabRow(fullLabels, shortLabels, 440f, measure));
        assertEquals(new HoloPanel.TabRowFit(fullLabels, 2), HoloPanel.fittingTabRow(fullLabels, shortLabels, 300f, measure));
        assertEquals(new HoloPanel.TabRowFit(shortLabels, 2), HoloPanel.fittingTabRow(fullLabels, shortLabels, 200f, measure));
        assertEquals(new HoloPanel.TabRowFit(shortLabels, 2), HoloPanel.fittingTabRow(fullLabels, shortLabels, 10f, measure));
        assertEquals(new HoloPanel.TabRowFit(List.of(), 0), HoloPanel.fittingTabRow(List.of(), List.of(), 0f, measure));
    }

    private static float charWidth(String font) {
        if (Fonts.ORBITRON_20AABOLD.equals(font)) {
            return 12f;
        }
        return Fonts.ORBITRON_20AA.equals(font) ? 10f : 6f;
    }
}
