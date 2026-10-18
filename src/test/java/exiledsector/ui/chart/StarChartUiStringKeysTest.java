package exiledsector.ui.chart;

import exiledsector.charts.ChartPreview;
import exiledsector.charts.ChartRunActions.OpenResult;
import exiledsector.i18n.RealCatalogue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StarChartUiStringKeysTest {

    private static final String UI_PREFIX = "chart.ui.";

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void everyKeyTheStarChartScreenBuildsExists(String language) {
        Map<String, String> catalogue = RealCatalogue.entries(language);
        List<String> missing = new ArrayList<>();
        for (WormholeOpenCheck.Blocker blocker : WormholeOpenCheck.Blocker.values()) {
            if (!blocker.allowsOpening()) {
                require(catalogue, blocker.reasonKey(), missing);
            }
        }
        for (StarChartCrafting.Refusal refusal : StarChartCrafting.Refusal.values()) {
            if (refusal != StarChartCrafting.Refusal.NONE) {
                require(catalogue, refusal.noticeKey(), missing);
            }
        }
        for (OpenResult result : OpenResult.values()) {
            require(catalogue, new StarChartOpening.Outcome(result, null).messageKey(), missing);
        }
        for (ChartPreview.Stat stat : ChartPreview.Stat.values()) {
            require(catalogue, StarChartPreviewText.statKey(stat), missing);
        }
        assertEquals(List.of(), missing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"zh_CN"})
    void everyStarChartScreenStringIsTranslated(String language) {
        Map<String, String> english = RealCatalogue.entries("en");
        Map<String, String> translated = RealCatalogue.entries(language);
        List<String> missing = new ArrayList<>();
        for (String key : english.keySet()) {
            if (key.startsWith(UI_PREFIX) && !translated.containsKey(key) && !translated.containsKey(otherForm(key))) {
                missing.add(key);
            }
        }
        assertEquals(List.of(), missing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void theSettingTextHasNoPercentSignForLunaLib(String language) {
        Map<String, String> catalogue = RealCatalogue.entries(language);
        for (String key : List.of("chart.ui.settings.openAnywhere.name", "chart.ui.settings.openAnywhere.tooltip")) {
            assertFalse(catalogue.get(key).contains("%"), key);
        }
    }

    private static String otherForm(String key) {
        return key.toLowerCase(Locale.ROOT).endsWith(".one") ? key.substring(0, key.length() - ".one".length()) + ".other" : key;
    }

    private static void require(Map<String, String> catalogue, String key, List<String> missing) {
        if (!catalogue.containsKey(key) && !catalogue.containsKey(key + ".other")) {
            missing.add(key);
        }
    }
}
