package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.tree.AtlasTreeType;
import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.i18n.RealCatalogue;
import exiledsector.i18n.StyledText;
import exiledsector.skills.SkillTier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeNodeTextTest {

    private static final String TREE = AtlasTreeNodeText.LINE_KEY_PREFIX;

    private static void useEnglishWith(Map<String, String> extraStrings) {
        Map<String, String> strings = new HashMap<>(RealCatalogue.entries("en"));
        strings.putAll(extraStrings);
        I18n.install(new Catalogue("en", strings));
    }

    private static List<String> body(AtlasTreeType type) {
        return AtlasTreeNodeText.body(type, null).stream().map(StyledText::plain).toList();
    }

    private static String chartLine(ChartModifier effect, float value) {
        return AtlasTreeNodeText.chartLine(effect, value).plain();
    }

    @Test
    void aTreeLineShowsItsOwnTextAndALineWithoutOneShowsTheChartLine() {
        AtlasTreeType type = new AtlasTreeType("lines", "Lines", null, SkillTier.SMALL,
                List.of(new AppliedEffect(ChartModifier.CHART_SPATIAL_BREACH_CHANCE_PERCENT, 8f), new AppliedEffect(ChartModifier.CHART_BOSS_STAR_CHART_PERCENT, 2f),
                        new AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 5f)),
                List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 1f)), null, List.of());

        assertEquals(List.of("Your Star Charts have +8% chance to contain a Spatial Breach",
                "Star Chart Bosses in your Star Charts drop 2% increased quantity of Star Charts",
                ChartModifier.CHART_ITEM_PERCENT.description(5f).plain(),
                AtlasModifier.ATLAS_VISIBILITY_LANES.description(1f).plain()), body(type));
    }

    @Test
    void theTreeLineTakesTheChartLinesOwnExtraArguments() {
        useEnglishWith(Map.of(ChartModifier.CHART_ARC_STRIKE_EMP_FLAT.key(), "chart {seconds} {value}",
                TREE + ChartModifier.CHART_ARC_STRIKE_EMP_FLAT.name(), "tree {seconds} {value}",
                ChartModifier.CHART_WEAPON_MISFIRE_SECONDS.key(), "chart {delay} {value}",
                TREE + ChartModifier.CHART_WEAPON_MISFIRE_SECONDS.name(), "tree {delay} {value}",
                ChartModifier.CHART_SENSOR_RANGE_PERCENT.key(), "chart {value}",
                TREE + ChartModifier.CHART_SENSOR_RANGE_PERCENT.name(), "tree {value}"));

        for (ChartModifier effect : List.of(ChartModifier.CHART_ARC_STRIKE_EMP_FLAT, ChartModifier.CHART_WEAPON_MISFIRE_SECONDS,
                ChartModifier.CHART_SENSOR_RANGE_PERCENT)) {
            String chart = effect.description(-30f).plain();
            assertEquals("tree" + chart.substring("chart".length()), chartLine(effect, -30f), effect.name());
        }
    }

    @Test
    void aNegativeLineUsesTheTreesNegativeTextAndFallsBackToTheChartsWhenTheTreeHasNone() {
        String relayCost = ChartModifier.CHART_RELAY_COST_PERCENT.name();
        useEnglishWith(Map.of(TREE + relayCost, "tree increased {value}", TREE + relayCost + ".reduced", "tree reduced {value}"));
        assertEquals("tree increased 10", chartLine(ChartModifier.CHART_RELAY_COST_PERCENT, 10f));
        assertEquals("tree reduced 10", chartLine(ChartModifier.CHART_RELAY_COST_PERCENT, -10f));

        useEnglishWith(Map.of(TREE + relayCost, "tree increased {value}"));
        assertEquals("tree increased 10", chartLine(ChartModifier.CHART_RELAY_COST_PERCENT, 10f));
        assertEquals(ChartModifier.CHART_RELAY_COST_PERCENT.description(-10f).plain(), chartLine(ChartModifier.CHART_RELAY_COST_PERCENT, -10f));
    }

    @Test
    void countedLinesFindTheirTreeTextUnderItsPluralForms() {
        String extraConvoys = ChartModifier.CHART_EXTRA_CONVOYS.name();
        String lanes = AtlasModifier.ATLAS_VISIBILITY_LANES.name();
        useEnglishWith(Map.of(TREE + extraConvoys + ".one", "{value} tree convoy", TREE + extraConvoys + ".other", "{value} tree convoys",
                TREE + lanes + ".one", "{value} tree lane", TREE + lanes + ".other", "{value} tree lanes"));

        assertEquals("1 tree convoy", chartLine(ChartModifier.CHART_EXTRA_CONVOYS, 1f));
        assertEquals("2 tree convoys", chartLine(ChartModifier.CHART_EXTRA_CONVOYS, 2f));
        assertEquals("2 tree lanes", AtlasTreeNodeText.atlasLine(AtlasModifier.ATLAS_VISIBILITY_LANES, 2f).plain());
    }

    @Test
    void everyLuckyLineCarriesTheReminderUnderItAndGoldenHourRaisesItsRollCount() {
        useEnglishWith(Map.of());
        AtlasTreeType type = new AtlasTreeType("perfect_light", "Perfect Light", null, SkillTier.KEYSTONE,
                List.of(new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_ALL_LUCKY, 1f),
                        new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_HULL_ITEM_PERCENT, -40f)),
                List.of(), null, List.of());

        assertEquals(List.of("Every modifier on hull items found in your Star Charts with Stellar Flares is lucky",
                "(Lucky modifiers are rolled twice, keeping the best)",
                "Your Star Charts with Stellar Flares have 40% reduced quantity of hull items found"), body(type));
        assertEquals("(Lucky modifiers are rolled 4 times, keeping the best)",
                AtlasTreeNodeText.body(type, null, 4).get(1).plain());
        assertEquals("Lucky modifiers in your Star Charts with Stellar Flares are rolled 4 times instead of twice, keeping the best",
                chartLine(ChartModifier.CHART_STELLAR_FLARES_LUCKY_ROLLS, 4f));
    }

    @Test
    void geomagneticSurgeAndSolarFrontTakeTheReminderWithGoldenHoursRollCount() {
        useEnglishWith(Map.of());
        AtlasTreeType type = new AtlasTreeType("surge_and_front", "Surge and Front", null, SkillTier.NOTABLE,
                List.of(new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS, 1f),
                        new AppliedEffect(ChartModifier.CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS, 1f)),
                List.of(), null, List.of());
        String reminder = "(Lucky modifiers are rolled 4 times, keeping the best)";

        List<String> lines = AtlasTreeNodeText.body(type, null, 4).stream().map(StyledText::plain).toList();

        assertEquals(List.of(chartLine(ChartModifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS, 1f), reminder,
                chartLine(ChartModifier.CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS, 1f), reminder), lines);
    }

    @Test
    void theLuckyLinesAreTheOnesThatMakeSomethingLucky() {
        assertTrue(ChartModifier.CHART_STELLAR_FLARES_LUCKY_MODIFIERS.makesLucky());
        assertTrue(ChartModifier.CHART_STELLAR_FLARES_LUCKY_IMPLICITS.makesLucky());
        assertTrue(ChartModifier.CHART_STELLAR_FLARES_ALL_LUCKY.makesLucky());
        assertFalse(ChartModifier.CHART_STELLAR_FLARES_LUCKY_ROLLS.makesLucky());
        assertFalse(ChartModifier.CHART_STELLAR_FLARES_HULL_ITEM_PERCENT.makesLucky());
        assertTrue(ChartModifier.CHART_STELLAR_FLARES_ALL_LUCKY.isFlag());
        assertFalse(ChartModifier.CHART_STELLAR_FLARES_LUCKY_MODIFIERS.isFlag());
        Map<String, String> chinese = RealCatalogue.entries("zh_CN");
        assertTrue(chinese.containsKey("chart.lucky.reminder") && chinese.containsKey("chart.lucky.reminder.twice"));
    }

    @Test
    void everyTravelAndChanceLineHasItsOwnTreeTextInEnglishAndChinese() {
        Map<String, String> english = RealCatalogue.entries("en");
        Map<String, String> chinese = RealCatalogue.entries("zh_CN");
        for (ChartModifier effect : List.of(ChartModifier.CHART_BOSS_STAR_CHART_PERCENT, ChartModifier.CHART_CATALYST_PERCENT,
                ChartModifier.CHART_MODIFIER_EFFECT_PERCENT, ChartModifier.CHART_SPATIAL_BREACH_CHANCE_PERCENT, ChartModifier.CHART_RELAY_NETWORK_CHANCE_PERCENT,
                ChartModifier.CHART_SUPPLY_CONVOY_CHANCE_PERCENT, ChartModifier.CHART_ANOMALOUS_DEBRIS_CHANCE_PERCENT,
                ChartModifier.CHART_EXCAVATION_SITES_CHANCE_PERCENT, ChartModifier.CHART_LISTENING_POST_CHANCE_PERCENT,
                ChartModifier.CHART_STELLAR_FLARES_CHANCE_PERCENT)) {
            String treeKey = TREE + effect.name();
            assertTrue(english.containsKey(treeKey) && chinese.containsKey(treeKey), treeKey);
            assertTrue(english.get(treeKey).contains("<good>{value}%</good>") || english.get(treeKey).contains("<good>+{value}%</good>"), treeKey);
        }
    }
}
