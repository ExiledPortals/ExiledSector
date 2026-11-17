package exiledsector.ui.cargo;

import exiledsector.items.crafting.material.InfusedFeedstock;

public final class InfusedFeedstockIconLooks implements ItemIconLooks {

    public static final InfusedFeedstockIconLooks INSTANCE = new InfusedFeedstockIconLooks();

    private InfusedFeedstockIconLooks() {
    }

    @Override
    public CargoIconLook materialLook(String itemId) {
        InfusedFeedstock feedstock = InfusedFeedstock.byItemId(itemId);
        return feedstock == null ? null : CargoIconLooks.forMaterial(InfusedFeedstock.TIER, feedstock.color()).withSwarm(SwarmMotion.ORBIT);
    }
}
