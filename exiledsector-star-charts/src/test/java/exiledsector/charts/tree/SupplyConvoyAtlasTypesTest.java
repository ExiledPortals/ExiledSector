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

class SupplyConvoyAtlasTypesTest {

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
    void theSupplyConvoyNodesCarryThePlansLines() throws Exception {
        AtlasTreeData data = shipped();

        assertType(data, "threat_feedstock_chance", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_CONVOY_THREAT_FEEDSTOCK_CHANCE_PERCENT, 10f));
        assertType(data, "shroud_feedstock_chance", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_CONVOY_SHROUD_FEEDSTOCK_CHANCE_PERCENT, 10f));
        assertType(data, "phase_feedstock_chance", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_CONVOY_PHASE_FEEDSTOCK_CHANCE_PERCENT, 10f));
        assertType(data, "feedstock_duplication", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_CONVOY_DOUBLE_FEEDSTOCK_CHANCE_PERCENT, 25f));
        assertType(data, "additional_convoy_chance", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_EXTRA_CONVOY_CHANCE_PERCENT, 30f));
        assertType(data, "threat_embargo", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_CONVOY_THREAT_FEEDSTOCK_CHANCE_PERCENT, 25f));
        assertType(data, "shroud_embargo", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_CONVOY_SHROUD_FEEDSTOCK_CHANCE_PERCENT, 25f));
        assertType(data, "phase_embargo", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_CONVOY_PHASE_FEEDSTOCK_CHANCE_PERCENT, 25f));
        assertType(data, "torn_holds", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_CONVOY_BREACH_FEEDSTOCK_PERCENT, 50f));
        assertType(data, "supply_lines", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_CONVOY_OFFICER_FEEDSTOCK, 1f));
        assertType(data, "bolt_hole_drop", SkillTier.NOTABLE,
                new AppliedEffect(ChartModifier.CHART_CONVOY_EXCAVATION_DELIVERY, 1f),
                new AppliedEffect(ChartModifier.CHART_CONVOY_EXCAVATION_RAID_DIFFICULTY_PERCENT, 100f),
                new AppliedEffect(ChartModifier.CHART_CONVOY_EXCAVATION_LOOT_PERCENT, 30f));
        assertType(data, "open_season", SkillTier.NOTABLE,
                new AppliedEffect(ChartModifier.CHART_HOSTILE_CONVOYS, 1f),
                new AppliedEffect(ChartModifier.CHART_CONVOY_FEEDSTOCK_PERCENT, 20f));
        assertType(data, "mixed_manifest", SkillTier.KEYSTONE,
                new AppliedEffect(ChartModifier.CHART_CONVOY_MIXED_MANIFEST, 1f),
                new AppliedEffect(ChartModifier.CHART_CONVOY_FLEET_SIZE_PERCENT, 20f));
    }
}
