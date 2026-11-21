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

class AnomalousDebrisTypesTest {

    private record Expected(String name, SkillTier tier, Map<ChartModifier, Float> effects) {
    }

    private static Map<String, Expected> expectedTypes() {
        Map<String, Expected> expected = new LinkedHashMap<>();
        expected.put("additional_debris", new Expected("Additional Debris", SkillTier.SMALL, Map.of(ChartModifier.CHART_EXTRA_ANOMALOUS_DEBRIS, 1f)));
        expected.put("engram_quantity", new Expected("Engram Quantity", SkillTier.SMALL,
                Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_CRAFTING_ITEM_QUANTITY_PERCENT, 20f)));
        expected.put("deep_corruption", new Expected("Deep Corruption", SkillTier.NOTABLE,
                Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_FLEET_SIZE_PERCENT, 30f, ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS, 1f)));
        expected.put("double_dose", new Expected("Double Dose", SkillTier.NOTABLE,
                Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_SECOND_CORRUPTION, 1f, ChartModifier.CHART_ANOMALOUS_DEBRIS_TWICE_CORRUPTED_ENGRAMS, 2f)));
        expected.put("lone_sleeper", new Expected("Lone Sleeper", SkillTier.NOTABLE, Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_SINGLE_FIELD, 1f,
                ChartModifier.CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT, 100f, ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS, 3f)));
        expected.put("breached_graveyard", new Expected("Breached Graveyard", SkillTier.NOTABLE, Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_BREACH_FIELD, 1f)));
        expected.put("restless_sleepers", new Expected("Restless Sleepers", SkillTier.NOTABLE,
                Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_AWAKE, 1f, ChartModifier.CHART_ANOMALOUS_DEBRIS_ROAMING, 1f)));
        expected.put("redacted_sleepers", new Expected("[REDACTED] Sleepers", SkillTier.NOTABLE,
                Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_REMNANT_SHIP_SHARE, 30f)));
        expected.put("threatening_sleepers", new Expected("Threatening Sleepers", SkillTier.NOTABLE,
                Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_THREAT_SHIP_SHARE, 30f)));
        expected.put("veiled_sleepers", new Expected("Veiled Sleepers", SkillTier.NOTABLE, Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_DWELLER_SHIP_SHARE, 30f)));
        expected.put("silent_choir", new Expected("Silent Choir", SkillTier.KEYSTONE,
                Map.of(ChartModifier.CHART_ANOMALOUS_DEBRIS_ALWAYS_TWICE_CORRUPTED, 1f, ChartModifier.CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT, 50f)));
        return expected;
    }

    private static AtlasTreeData shipped() throws Exception {
        return AtlasTreeData.parse(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH)),
                ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TREE_PATH)));
    }

    @Test
    void everyAnomalousDebrisNodeShipsWithItsTierAndLines() throws Exception {
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
                assertTrue(text.contains("<good>") || text.contains("<bad>"), lineKey);
                assertEquals(!effect.isFlag(), text.contains("{value}"), lineKey);
            }
        });
    }

    @Test
    void theTreeOnlyLinesNeverRollOnStarCharts() throws Exception {
        String rolledModifiers = Files.readString(ProjectFiles.projectRoot().resolve("data/config/exiledSector/star_chart_modifiers.csv"));
        for (String effectName : List.of("CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT", "CHART_ANOMALOUS_DEBRIS_CORRUPTED", "CHART_ANOMALOUS_DEBRIS_TWICE",
                "CHART_ANOMALOUS_DEBRIS_SECOND", "CHART_ANOMALOUS_DEBRIS_ALWAYS", "CHART_ANOMALOUS_DEBRIS_SINGLE", "CHART_ANOMALOUS_DEBRIS_BREACH",
                "CHART_ANOMALOUS_DEBRIS_AWAKE", "CHART_ANOMALOUS_DEBRIS_ROAMING", "_SHIP_SHARE")) {
            assertFalse(rolledModifiers.contains(effectName), effectName);
        }
    }
}
