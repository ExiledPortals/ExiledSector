package exiledsector.items;

import java.util.List;

public record CraftCost(List<String> feedstockIds, int feedstockAmount, int reshapingCost) {

    public CraftCost {
        feedstockIds = List.copyOf(feedstockIds);
    }

    public static String reshapingId() {
        return Nanoswarm.RESHAPING.itemId();
    }

    public boolean canAfford(CraftingMaterials materials) {
        return feedstockIds.stream().allMatch(feedstockId -> materials.count(feedstockId) >= feedstockAmount)
                && materials.count(reshapingId()) >= reshapingCost;
    }

    void takeFrom(CraftingMaterials materials) {
        feedstockIds.forEach(feedstockId -> materials.take(feedstockId, feedstockAmount));
        materials.take(reshapingId(), reshapingCost);
    }
}
