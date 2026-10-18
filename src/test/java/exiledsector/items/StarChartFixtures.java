package exiledsector.items;

import exiledsector.charts.ChartEffects;

public final class StarChartFixtures {

    private StarChartFixtures() {
    }

    public static void registerShipped() throws Exception {
        ChartEffects.register();
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
        StarChartItems.clear();
    }
}
