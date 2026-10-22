package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.Fonts;
import exiledlib.i18n.RealCatalogue;
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
    void theItemsPanelHasHullItemsThenHullUpgrades() {
        assertEquals(List.of(SocketPanelTab.HULL_ITEMS, SocketPanelTab.HULL_UPGRADES), List.of(SocketPanelTab.values()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void everyTabLabelIsTranslated(String language) {
        Map<String, String> catalogue = RealCatalogue.entries(language);
        for (SocketPanelTab tab : SocketPanelTab.values()) {
            assertNotNull(catalogue.get(tab.labelKey()), tab + " in " + language);
        }
    }

    @Test
    void theTabRowFallsBackToNarrowerFontsWhenThreeTabsDoNotFitBesideClose() {
        List<String> labels = List.of("Hull Items", "Hull Upgrades", "Star Charts");
        BiFunction<String, String, Float> measure = (label, font) -> label.length() * charWidth(font);

        assertEquals(0, HoloPanel.fittingTabFontIndex(labels, 1000f, measure));
        assertEquals(1, HoloPanel.fittingTabFontIndex(labels, 420f, measure));
        assertEquals(2, HoloPanel.fittingTabFontIndex(labels, 300f, measure));
        assertEquals(2, HoloPanel.fittingTabFontIndex(labels, 10f, measure));
        assertEquals(0, HoloPanel.fittingTabFontIndex(List.of(), 0f, measure));
    }

    private static float charWidth(String font) {
        if (Fonts.ORBITRON_20AABOLD.equals(font)) {
            return 12f;
        }
        return Fonts.ORBITRON_20AA.equals(font) ? 10f : 6f;
    }
}
