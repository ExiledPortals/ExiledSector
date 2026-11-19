package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.skills.SkillTier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DenseNebulaAtlasTypesTest {

    private static final String CHART_MODIFIERS_PATH = "data/config/exiledSector/star_chart_modifiers.csv";
    private static final String ATLAS_CHANCE_TYPE = "dense_nebula_atlas_chance";

    private static final Map<String, List<ChartModifier>> CHART_EFFECTS = Map.ofEntries(
            Map.entry("settling_dust_effect", List.of(ChartModifier.CHART_SETTLING_DUST_EFFECT_PERCENT)),
            Map.entry("settling_dust_stacks", List.of(ChartModifier.CHART_SETTLING_DUST_MAX_STACKS)),
            Map.entry("dust_trails", List.of(ChartModifier.CHART_SETTLING_DUST_QUANTITY_PERCENT)),
            Map.entry("long_settling", List.of(ChartModifier.CHART_SETTLING_DUST_MAX_STACKS, ChartModifier.CHART_SETTLING_DUST_FLEET_SIZE_MULT)),
            Map.entry("ionized_dust", List.of(ChartModifier.CHART_SETTLING_DUST_IONIZED)));
    private static final Set<String> SMALL_TYPES = Set.of("settling_dust_effect", "settling_dust_stacks");

    @Test
    void everyDenseNebulaNodeTypeCarriesItsLines() throws Exception {
        JSONObject types = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH));
        Map<String, AtlasTreeType> parsed = AtlasTreeData.parseTypes(types);

        for (Map.Entry<String, List<ChartModifier>> expected : CHART_EFFECTS.entrySet()) {
            AtlasTreeType type = parsed.get(expected.getKey());
            assertNotNull(type, expected.getKey());
            assertEquals(expected.getValue(), type.effects().stream().map(ChartGenContext.AppliedEffect::effect).toList(), expected.getKey());
            assertTrue(type.effects().stream().allMatch(line -> line.value() != 0f), expected.getKey());
            assertEquals(SMALL_TYPES.contains(expected.getKey()) ? SkillTier.SMALL : SkillTier.NOTABLE, type.tier(), expected.getKey());
        }
        AtlasTreeType chance = parsed.get(ATLAS_CHANCE_TYPE);
        assertNotNull(chance);
        assertEquals(SkillTier.SMALL, chance.tier());
        assertTrue(chance.effects().isEmpty());
        assertEquals(List.of(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT), chance.atlasEffects().stream().map(AtlasEffects.Line::effect).toList());
    }

    @Test
    void theSettlingDustLinesNeverRollOnCharts() throws Exception {
        JSONArray rows = ProjectFiles.csvRows(CHART_MODIFIERS_PATH);
        for (int i = 0; i < rows.length(); i++) {
            String lines = rows.getJSONObject(i).optString("lines");
            for (List<ChartModifier> treeLines : CHART_EFFECTS.values()) {
                for (ChartModifier line : treeLines) {
                    assertFalse(lines.contains(line.name() + ":"), line.name());
                }
            }
        }
    }
}
