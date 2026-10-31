package exiledsector.ui.chart;

import exiledsector.charts.run.ChartRunActions.OpenResult;
import exiledsector.i18n.RealCatalogue;
import exiledsector.charts.anomaly.hypershunt.CoronalHypershunt;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.RedeploymentCatalystUse;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.UseVerdict;
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
    void everyKeyTheStarChartsTabBuildsExists(String language) {
        Map<String, String> catalogue = RealCatalogue.entries(language);
        List<String> missing = new ArrayList<>();
        for (WormholeOpenCheck.Blocker blocker : WormholeOpenCheck.Blocker.values()) {
            if (!blocker.allowsOpening()) {
                require(catalogue, blocker.reasonKey(), missing);
            }
        }
        for (UseVerdict refusal : StarChartCrafting.REFUSALS) {
            require(catalogue, refusal.refusalKey(), missing);
        }
        for (AtlasAnomalies.InsertCheck check : AtlasAnomalies.InsertCheck.values()) {
            if (!check.allows()) {
                require(catalogue, AtlasCatalystUse.refusalKey(check), missing);
            }
        }
        for (String key : List.of(AtlasCatalystUse.FOGGED_KEY, RedeploymentCatalystUse.SPECIAL_KEY, RedeploymentCatalystUse.INFESTED_KEY,
                RedeploymentCatalystUse.NO_FACTION_KEY, FactionSkirmish.NO_SIDE_KEY, FactionSkirmish.NO_ATTACKER_KEY, CoronalHypershunt.TOO_CLOSE_KEY, "catalyst.redeploys", "catalyst.target.redeploy", "catalyst.confirm.redeploy",
                "catalyst.window.redeployed")) {
            require(catalogue, key, missing);
        }
        for (OpenResult result : OpenResult.values()) {
            require(catalogue, new StarChartOpening.Outcome(result, null).messageKey(), missing);
        }
        for (String key : List.of("chart.ui.currency.alsoStarCharts", "ui.workbench.confirm.useTitle", "ui.workbench.confirm.growTitle", "ui.workbench.confirm.growBody",
                "ui.workbench.confirm.skipHint", "ui.workbench.confirm.use", "ui.workbench.confirm.grow", "ui.workbench.confirm.cancel")) {
            require(catalogue, key, missing);
        }
        assertEquals(List.of(), missing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void everyChartCurrencyHasItsNameDescriptionAndRecipeText(String language) {
        Map<String, String> catalogue = RealCatalogue.entries(language);
        List<String> missing = new ArrayList<>();
        for (StarChartCurrency currency : StarChartCurrency.values()) {
            require(catalogue, "material." + currency.itemId() + ".name", missing);
            require(catalogue, "material." + currency.itemId() + ".description", missing);
            require(catalogue, "chart.currency." + currency.key() + ".description", missing);
        }
        for (CraftingCurrency kernel : StarChartCurrency.KERNELS_ON_CHARTS) {
            require(catalogue, "chart.currency." + kernel.key() + ".description", missing);
        }
        require(catalogue, "chart.drop.reward", missing);
        assertEquals(List.of(), missing);
        for (String key : catalogue.keySet()) {
            if (key.startsWith("chart.currency.") || key.startsWith("chart.drop.")) {
                assertFalse(catalogue.get(key).contains("%"), key);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"zh_CN"})
    void everyStarChartsTabStringIsTranslated(String language) {
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
        for (String key : List.of("chart.ui.settings.openAnywhere.name", "chart.ui.settings.openAnywhere.tooltip",
                "chart.ui.settings.debugTools.name", "chart.ui.settings.debugTools.tooltip")) {
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
