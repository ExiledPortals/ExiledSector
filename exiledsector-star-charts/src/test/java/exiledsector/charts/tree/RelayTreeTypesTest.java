package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.skills.SkillTier;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RelayTreeTypesTest {

    private static final Map<String, List<ChartGenContext.AppliedEffect>> TYPES = Map.of(
            "relay_reward_rarity", List.of(line(ChartModifier.CHART_RELAY_REWARD_RARITY_PERCENT, 10f)),
            "forked_trace", List.of(line(ChartModifier.CHART_RELAY_ONWARD_SYSTEMS, 2f)),
            "crowded_channel", List.of(line(ChartModifier.CHART_RELAY_EXTRA_CHOICES, 1f)),
            "double_down", List.of(line(ChartModifier.CHART_RELAY_EXTRA_REWARD_COPIES, 1f), line(ChartModifier.CHART_RELAY_EXTRA_SCARS, 1f)),
            "extra_relay", List.of(line(ChartModifier.CHART_RELAY_EXTRA_RELAYS, 1f)),
            "scar_collector", List.of(line(ChartModifier.CHART_RELAY_SCAR_REWARDS, 1f)),
            "early_echo", List.of(line(ChartModifier.CHART_RELAY_STAGE_UNIQUE_CHANCE, 15f)),
            "shared_broadcast", List.of(line(ChartModifier.CHART_RELAY_SHARED_BROADCAST, 1f)),
            "full_array", List.of(line(ChartModifier.CHART_RELAY_FULL_ARRAY, 1f)));

    private static ChartGenContext.AppliedEffect line(ChartModifier effect, float value) {
        return new ChartGenContext.AppliedEffect(effect, value);
    }

    @Test
    void everyRelayNetworkNodeTypeShipsWithItsLinesAndTier() throws Exception {
        JSONObject types = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH));
        JSONObject tree = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TREE_PATH));
        AtlasTreeData data = AtlasTreeData.parse(types, tree);

        TYPES.forEach((id, lines) -> {
            AtlasTreeType type = data.type(id);
            assertNotNull(type, id);
            assertEquals(lines, type.effects(), id);
            assertEquals(id.equals("relay_reward_rarity") ? SkillTier.SMALL : SkillTier.NOTABLE, type.tier(), id);
        });
    }

    @Test
    void theRelayLinesReadAsTheChartsOwnText() {
        assertEquals("Each relay choice in the system adds its reward twice", ChartModifier.CHART_RELAY_EXTRA_REWARD_COPIES.description(1f).plain());
        assertEquals("Each relay choice in the system adds an additional scar", ChartModifier.CHART_RELAY_EXTRA_SCARS.description(1f).plain());
        assertEquals("Relays in the system offer 1 additional choice", ChartModifier.CHART_RELAY_EXTRA_CHOICES.description(1f).plain());
        assertEquals("10% increased rarity of items from relay rewards in the system",
                ChartModifier.CHART_RELAY_REWARD_RARITY_PERCENT.description(10f).plain());
        assertEquals("Waking every relay in the system reveals all enemy fleets in it on the map and minimap",
                ChartModifier.CHART_RELAY_FULL_ARRAY.description(1f).plain());
    }
}
