package exiledsector.charts.tree;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.skills.SkillTier;
import exiledsector.skills.layout.TreeTags;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeEffectGroupsTest {

    private static final AtlasTreeType BOSS_CHARTS = type(List.of(new AppliedEffect(ChartModifier.CHART_BOSS_STAR_CHART_PERCENT, 2f)), List.of());
    private static final AtlasTreeType BREACH_CHANCE = type(List.of(new AppliedEffect(ChartModifier.CHART_SPATIAL_BREACH_CHANCE_PERCENT, 8f)), List.of());
    private static final AtlasTreeType DEBRIS_CHANCE = type(List.of(new AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_CHANCE_PERCENT, 8f)), List.of());
    private static final AtlasTreeType LANES = type(List.of(), List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 1f)));
    private static final AtlasTreeType NOTHING = type(List.of(), List.of());

    private static int nodeCount;

    private static AtlasTreeType type(List<AppliedEffect> chartLines, List<AtlasEffects.Line> atlasLines) {
        return new AtlasTreeType("type", "Type", null, SkillTier.SMALL, chartLines, atlasLines, null, List.of());
    }

    private static AtlasTreeNode node(AtlasTreeType type, String... tags) {
        return new AtlasTreeNode("node_" + nodeCount++, type, List.of(), 0f, 0f, null, new TreeTags(List.of(tags), null));
    }

    @Test
    void untaggedNodesComeFirstThenSingleAnomaliesThenHybridsWithEachGroupsLinesSummed() {
        List<AtlasTreeEffectGroups.Group> groups = AtlasTreeEffectGroups.of(List.of(
                node(BREACH_CHANCE, "spatial_breach"),
                node(DEBRIS_CHANCE, "anomalous_debris", "spatial_breach"),
                node(BOSS_CHARTS),
                node(BREACH_CHANCE, "spatial_breach"),
                node(BOSS_CHARTS),
                node(LANES, "spatial_breach", "anomalous_debris")));

        assertEquals(List.of(List.of(), List.of("spatial_breach"), List.of("anomalous_debris", "spatial_breach")),
                groups.stream().map(AtlasTreeEffectGroups.Group::anomalyIds).toList());
        assertTrue(groups.get(0).everyChart());
        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_BOSS_STAR_CHART_PERCENT, 4f)), groups.get(0).chartLines());
        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_SPATIAL_BREACH_CHANCE_PERCENT, 16f)), groups.get(1).chartLines());
        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_CHANCE_PERCENT, 8f)), groups.get(2).chartLines());
        assertEquals(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 1f)), groups.get(2).atlasLines());
    }

    @Test
    void tagsThatAreNotAnomaliesAreIgnoredAndNodesWithoutEffectsLeaveNoGroup() {
        List<AtlasTreeEffectGroups.Group> groups = AtlasTreeEffectGroups.of(List.of(
                node(BOSS_CHARTS, "not_an_anomaly"),
                node(NOTHING, "relay_network"),
                node(null, "spatial_breach")));

        assertEquals(1, groups.size());
        assertTrue(groups.get(0).everyChart());
    }

    @Test
    void nothingAllocatedGivesNoGroups() {
        assertTrue(AtlasTreeEffectGroups.of(List.of()).isEmpty());
    }
}
