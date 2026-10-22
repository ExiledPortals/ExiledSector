package exiledsector.items;

import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledsector.charts.ChartEffects;
import exiledsector.charts.ChartFactions;

import java.util.List;

public final class StarChartFixtures {

    static final String CHART_FACTIONS_PATH = "data/config/exiledSector/chart_factions.csv";

    private StarChartFixtures() {
    }

    public static void registerShipped() throws Exception {
        ChartEffects.register();
        ChartFactions.register(ItemFixtures.shippedRows(CHART_FACTIONS_PATH));
        StarChartItems.register(ItemFixtures.shippedRows(StarChartItems.MODIFIERS_PATH), ItemFixtures.shippedRows(StarChartImplicits.DATA_PATH),
                ItemFixtures.shippedRows(StarChartDefinitions.DATA_PATH));
    }

    public static ItemDefinition definition(String definitionId) {
        return ItemDefinitions.get(definitionId);
    }

    public static StarChart chart(String definitionId, long seed, int tier) {
        return (StarChart) ItemData.rolledAtTier(ItemDefinitions.get(definitionId), seed, tier).preview();
    }

    public static void clear() {
        StarChartImplicits.registerSharedChoice(List.of());
        StarChartItems.clear();
    }
}
