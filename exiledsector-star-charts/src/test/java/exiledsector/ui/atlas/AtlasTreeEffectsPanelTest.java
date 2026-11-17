package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.tree.AtlasTreeEffectGroups;
import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.i18n.RealCatalogue;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeEffectsPanelTest {

    @BeforeEach
    void useEnglish() {
        I18n.install(new Catalogue("en", RealCatalogue.entries("en")));
    }

    private static AtlasTreeEffectGroups.Group group(List<String> anomalyIds) {
        return new AtlasTreeEffectGroups.Group(anomalyIds, List.of(), List.of());
    }

    @Test
    void aGroupIsNamedForItsAnomaliesOrForEveryChartWhenItHasNone() {
        assertEquals(Translation.text("atlasTree.effects.everyChart"), AtlasTreeEffectsPanel.groupName(group(List.of())));
        assertEquals(AtlasNames.anomaly("spatial_breach"), AtlasTreeEffectsPanel.groupName(group(List.of("spatial_breach"))));
        assertEquals(AtlasNames.anomaly("anomalous_debris") + " and " + AtlasNames.anomaly("spatial_breach"),
                AtlasTreeEffectsPanel.groupName(group(List.of("anomalous_debris", "spatial_breach"))));
    }

    @Test
    void aGroupShowsItsChartLinesThenItsAtlasLinesAsTheTreeWordsThem() {
        AtlasTreeEffectGroups.Group group = new AtlasTreeEffectGroups.Group(List.of("spatial_breach"),
                List.of(new AppliedEffect(ChartModifier.CHART_SPATIAL_BREACH_CHANCE_PERCENT, 16f)),
                List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 2f)));

        assertEquals(List.of(AtlasTreeNodeText.chartLine(ChartModifier.CHART_SPATIAL_BREACH_CHANCE_PERCENT, 16f).plain(),
                        AtlasTreeNodeText.atlasLine(AtlasModifier.ATLAS_VISIBILITY_LANES, 2f).plain()),
                AtlasTreeEffectsPanel.lines(group).stream().map(StyledText::plain).toList());
    }

    @Test
    void everyPanelStringIsInEnglishAndChinese() {
        Map<String, String> english = RealCatalogue.entries("en");
        Map<String, String> chinese = RealCatalogue.entries("zh_CN");
        for (String key : List.of("title", "show", "hide", "everyChart", "hybrid", "none")) {
            String fullKey = "atlasTree.effects." + key;
            assertTrue(english.containsKey(fullKey) && chinese.containsKey(fullKey), fullKey);
        }
    }
}
