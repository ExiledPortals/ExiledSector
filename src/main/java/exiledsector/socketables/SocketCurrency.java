package exiledsector.socketables;

public enum SocketCurrency {

    AUGMENTATION("exiledSector_augmentation_kernel", 10, 6),
    RECALIBRATION("exiledSector_recalibration_kernel", 20, 3),
    TRANSPOSITION("exiledSector_transposition_kernel", 100, 1);

    private final String commodityId;
    private final int defaultPartsCost;
    private final float dropWeight;

    SocketCurrency(String commodityId, int defaultPartsCost, float dropWeight) {
        this.commodityId = commodityId;
        this.defaultPartsCost = defaultPartsCost;
        this.dropWeight = dropWeight;
    }

    public String commodityId() {
        return commodityId;
    }

    public float dropWeight() {
        return dropWeight;
    }

    public int partsCost() {
        return SocketCraftingCosts.cost(commodityId, defaultPartsCost);
    }
}
