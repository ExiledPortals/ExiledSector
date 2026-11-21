package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.skills.SkillTier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcavationAtlasTypesTest {

    private static final String CHART_MODIFIERS_PATH = "data/config/exiledSector/star_chart_modifiers.csv";

    private static final Map<String, List<ChartModifier>> EFFECTS = Map.ofEntries(
            Map.entry("additional_site", List.of(ChartModifier.CHART_EXTRA_EXCAVATION_SITES)),
            Map.entry("inhibitor_quantity", List.of(ChartModifier.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT)),
            Map.entry("open_bores", List.of(ChartModifier.CHART_EXCAVATION_OPEN_SHAFTS, ChartModifier.CHART_EXCAVATION_GUARDIAN_INHIBITORS)),
            Map.entry("sapper_teams", List.of(ChartModifier.CHART_EXCAVATION_COLLAPSED_SHAFTS, ChartModifier.CHART_EXCAVATION_RAID_INHIBITORS)),
            Map.entry("twin_shafts", List.of(ChartModifier.CHART_EXCAVATION_TWIN_SHAFTS)),
            Map.entry("deeper_bores", List.of(ChartModifier.CHART_EXCAVATION_SECOND_CAVERN, ChartModifier.CHART_EXCAVATION_SECOND_CAVERN_FLEET_SIZE_MULT,
                    ChartModifier.CHART_EXCAVATION_SECOND_CAVERN_INHIBITORS)),
            Map.entry("needle_bores", List.of(ChartModifier.CHART_EXCAVATION_FRIGATES_ONLY, ChartModifier.CHART_EXCAVATION_GUARDIAN_INHIBITORS)),
            Map.entry("widened_bores", List.of(ChartModifier.CHART_EXCAVATION_CRUISERS_ALLOWED, ChartModifier.CHART_EXCAVATION_GUARDIAN_INHIBITORS)),
            Map.entry("pitch_black", List.of(ChartModifier.CHART_EXCAVATION_SIGHT_RADIUS_PERCENT, ChartModifier.CHART_EXCAVATION_OPEN_SHAFT_INHIBITOR_PERCENT)),
            Map.entry("infested_bolt_holes", List.of(ChartModifier.CHART_EXCAVATION_CORRUPTIBLE_GUARDIANS,
                    ChartModifier.CHART_EXCAVATION_CORRUPTED_GUARDIAN_INHIBITORS)),
            Map.entry("settled_shafts", List.of(ChartModifier.CHART_EXCAVATION_INHIBITOR_PERCENT_PER_DUST)));

    @Test
    void everyExcavationNodeTypeCarriesItsLines() throws Exception {
        JSONObject types = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH));
        Map<String, AtlasTreeType> parsed = AtlasTreeData.parseTypes(types);

        for (Map.Entry<String, List<ChartModifier>> expected : EFFECTS.entrySet()) {
            AtlasTreeType type = parsed.get(expected.getKey());
            assertNotNull(type, expected.getKey());
            assertEquals(expected.getValue(), type.effects().stream().map(ChartGenContext.AppliedEffect::effect).toList(), expected.getKey());
            assertTrue(type.effects().stream().allMatch(line -> line.value() != 0f), expected.getKey());
            boolean small = expected.getKey().equals("additional_site") || expected.getKey().equals("inhibitor_quantity");
            assertEquals(small ? SkillTier.SMALL : SkillTier.NOTABLE, type.tier(), expected.getKey());
        }
    }

    @Test
    void theExcavationLinesNeverRollOnCharts() throws Exception {
        JSONArray rows = ProjectFiles.csvRows(CHART_MODIFIERS_PATH);
        for (int i = 0; i < rows.length(); i++) {
            String lines = rows.getJSONObject(i).optString("lines");
            for (List<ChartModifier> treeLines : EFFECTS.values()) {
                for (ChartModifier line : treeLines) {
                    assertFalse(lines.contains(line.name() + ":"), line.name());
                }
            }
        }
    }
}
