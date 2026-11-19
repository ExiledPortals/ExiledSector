package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.i18n.RealCatalogue;
import exiledsector.skills.SkillTier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagneticStormAtlasTypesTest {

    private record Expected(String name, SkillTier tier, Map<ChartModifier, Float> effects) {
    }

    private static Map<String, Expected> expectedTypes() {
        Map<String, Expected> expected = new LinkedHashMap<>();
        expected.put("magnetic_storm_chance", new Expected("Magnetic Storm Chance", SkillTier.SMALL,
                Map.of(ChartModifier.CHART_MAGNETIC_STORM_CHANCE_PERCENT, 8f)));
        expected.put("storm_crafting_items", new Expected("Storm Crafting Items", SkillTier.SMALL,
                Map.of(ChartModifier.CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT, 10f)));
        expected.put("charged_fields", new Expected("Charged Fields", SkillTier.NOTABLE, Map.of(ChartModifier.CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT, 30f)));
        expected.put("static_harvest", new Expected("Static Harvest", SkillTier.NOTABLE, Map.of(ChartModifier.CHART_MAGNETIC_STORM_FEEDSTOCK_PERCENT, 30f)));
        expected.put("ion_front", new Expected("Ion Front", SkillTier.NOTABLE, Map.of(ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT, 1f,
                ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT, 50f)));
        expected.put("geomagnetic_surge", new Expected("Geomagnetic Surge", SkillTier.NOTABLE, Map.of(ChartModifier.CHART_MAGNETIC_STORM_FLARE_AURORAS, 1f,
                ChartModifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS, 1f)));
        return expected;
    }

    private static AtlasTreeData shipped() throws Exception {
        return AtlasTreeData.parse(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH)),
                ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TREE_PATH)));
    }

    @Test
    void everyMagneticStormNodeShipsWithItsTierAndLines() throws Exception {
        AtlasTreeData data = shipped();

        expectedTypes().forEach((typeId, expected) -> {
            AtlasTreeType type = data.type(typeId);
            assertNotNull(type, typeId);
            assertEquals(expected.name(), type.name(), typeId);
            assertEquals(expected.tier(), type.tier(), typeId);
            assertNull(type.iconPath(), typeId);
            Map<ChartModifier, Float> effects = new LinkedHashMap<>();
            for (ChartGenContext.AppliedEffect line : type.effects()) {
                effects.put(line.effect(), line.value());
            }
            assertEquals(expected.effects(), effects, typeId);
        });
        assertEquals("magnetic_storm", data.type("magnetic_storm_chance").effects().get(0).effect().openingChanceAnomalyId());
    }

    @Test
    void everyLineTheNodesUseHasTreeTextAndEveryNodeAChineseName() {
        Map<String, String> english = RealCatalogue.entries("en");
        Map<String, String> chinese = RealCatalogue.entries("zh_CN");

        expectedTypes().forEach((typeId, expected) -> {
            assertTrue(chinese.containsKey("atlasTree.type." + typeId + ".name"), typeId);
            for (ChartModifier effect : expected.effects().keySet()) {
                String lineKey = "atlasTree.line." + effect.name();
                assertTrue(english.containsKey(lineKey) || english.containsKey(lineKey + ".one") && english.containsKey(lineKey + ".other"), lineKey);
                assertTrue(chinese.containsKey(lineKey) || chinese.containsKey(lineKey + ".other"), lineKey);
                String text = english.getOrDefault(lineKey, english.get(lineKey + ".other"));
                assertTrue(text.toLowerCase(Locale.ROOT).contains("your star charts"), lineKey);
                assertTrue(text.contains("<good>"), lineKey);
                assertEquals(!effect.isFlag(), text.contains("{value}"), lineKey);
            }
        });
    }

    @Test
    void theLuckyLineCarriesTheLuckyReminder() {
        Map<String, String> english = RealCatalogue.entries("en");
        Map<String, String> chinese = RealCatalogue.entries("zh_CN");
        String reminder = english.get("chart.atlas.anomaly.stellar_flares.reward");
        String chineseReminder = chinese.get("chart.atlas.anomaly.stellar_flares.reward");

        for (String key : List.of("atlasTree.line.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS.one", "atlasTree.line.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS.other",
                "chart.modifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS.other")) {
            assertTrue(english.get(key).endsWith(reminder.substring(reminder.indexOf('\n'))), key);
        }
        for (String key : List.of("atlasTree.line.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS.other", "chart.modifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS.other")) {
            assertTrue(chinese.get(key).endsWith(chineseReminder.substring(chineseReminder.indexOf('\n'))), key);
        }
    }

    @Test
    void theTreeOnlyLinesNeverRollOnStarCharts() throws Exception {
        String rolledModifiers = Files.readString(ProjectFiles.projectRoot().resolve("data/config/exiledSector/star_chart_modifiers.csv"));

        assertFalse(rolledModifiers.contains("CHART_MAGNETIC_STORM_"));
    }
}
