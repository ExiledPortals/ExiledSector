package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.skills.SkillTier;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StormFrontAtlasTypesTest {

    private static AtlasTreeData shipped() throws Exception {
        JSONObject types = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH));
        JSONObject tree = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TREE_PATH));
        return AtlasTreeData.parse(types, tree);
    }

    private static void assertType(AtlasTreeData data, String typeId, SkillTier tier, AppliedEffect... effects) {
        AtlasTreeType type = data.type(typeId);
        assertNotNull(type, typeId);
        assertEquals(tier, type.tier(), typeId);
        assertEquals(List.of(effects), type.effects(), typeId);
        assertNull(type.iconPath(), typeId + " has no icon");
    }

    @Test
    void theStormFrontNodesCarryThePlansLines() throws Exception {
        AtlasTreeData data = shipped();

        assertType(data, "storm_front_chance", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_CHANCE_PERCENT, 8f));
        assertType(data, "storm_item_quantity", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT, 15f));
        assertType(data, "storm_front_size", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_SIZE_PERCENT, 20f));
        assertType(data, "lightning_rod", SkillTier.NOTABLE,
                new AppliedEffect(ChartModifier.CHART_STORM_STRIKE_FREQUENCY_PERCENT, 50f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT, 30f));
        assertType(data, "supercell", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_SIZE_PERCENT, 100f));
        assertType(data, "eye_of_the_storm", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_HELD_SPEED_PERCENT, 80f));
        assertType(data, "redacted_front", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_REDACTED_FRIGATES, 10f));
        assertType(data, "captured_front", SkillTier.NOTABLE,
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_CAPTURED, 1f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_CAPTURED_IMMUTABLE_CHANCE_PERCENT, 10f));
        assertType(data, "solar_front", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS, 1f));
        assertType(data, "thunder_duel", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_STORM_FRONT_OFFICER_EXTRA_REWARDS, 1f));
    }
}
