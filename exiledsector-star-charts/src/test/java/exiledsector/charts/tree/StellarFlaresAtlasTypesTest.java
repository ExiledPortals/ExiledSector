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

class StellarFlaresAtlasTypesTest {

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
    void theStellarFlaresNodesCarryThePlansLines() throws Exception {
        AtlasTreeData data = shipped();

        assertType(data, "stellar_flares_chance", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_CHANCE_PERCENT, 8f));
        assertType(data, "flare_hull_items", SkillTier.SMALL, new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_HULL_ITEM_PERCENT, 10f));
        assertType(data, "solar_maximum", SkillTier.NOTABLE,
                new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_LUCKY_MODIFIERS, 1f),
                new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_FLARE_FREQUENCY_MULT, 50f));
        assertType(data, "tempered_light", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_LUCKY_IMPLICITS, 1f));
        assertType(data, "golden_hour", SkillTier.NOTABLE, new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_LUCKY_ROLLS, 4f));
        assertType(data, "sunstruck_convoy", SkillTier.NOTABLE,
                new AppliedEffect(ChartModifier.CHART_CONVOY_SUNSTRUCK, 1f),
                new AppliedEffect(ChartModifier.CHART_CONVOY_SUNSTRUCK_COMBAT_READINESS_PERCENT, -20f));
        assertType(data, "perfect_light", SkillTier.KEYSTONE,
                new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_ALL_LUCKY, 1f),
                new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_HULL_ITEM_PERCENT, -40f));
    }
}
