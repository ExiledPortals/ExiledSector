package exiledsector.ui.cargo;

import exiledsector.items.InfusedFeedstock;
import exiledsector.items.Nanoswarm;

public final class InfusedFeedstockIconLooks implements ItemIconLooks {

    public static final InfusedFeedstockIconLooks INSTANCE = new InfusedFeedstockIconLooks();

    private InfusedFeedstockIconLooks() {
    }

    @Override
    public CargoIconLook materialLook(String itemId) {
        InfusedFeedstock feedstock = InfusedFeedstock.byItemId(itemId);
        return feedstock == null ? null : CargoIconLooks.forMaterial(Nanoswarm.TUNING.tier(), feedstock.color()).withSwarm(SwarmMotion.ORBIT);
    }
}
