package exiledsector.items;

import java.awt.Color;

public enum CraftingCurrency {

    AUGMENTATION("exiledSector_augmentation_kernel", 10, 6, MaterialTier.MINOR, new Color(90, 170, 255)),
    ATTUNEMENT("exiledSector_attunement_kernel", 10, 4, MaterialTier.MINOR, new Color(185, 125, 255)),
    RECALIBRATION("exiledSector_recalibration_kernel", 20, 3, MaterialTier.MAJOR, new Color(110, 235, 140)),
    SUBSTITUTION("exiledSector_substitution_kernel", 20, 3, MaterialTier.MAJOR, new Color(255, 195, 80)),
    TRANSPOSITION("exiledSector_transposition_kernel", 100, 1, MaterialTier.EXOTIC, new Color(255, 140, 50)),
    FIXATION("exiledSector_fixation_kernel", 100, 1, MaterialTier.EXOTIC, new Color(255, 75, 70)),
    REPLICATION("exiledSector_replication_kernel", 0, 0.035f, MaterialTier.MYTHIC, new Color(225, 240, 255));

    private final String itemId;
    private final int defaultPartsCost;
    private final float dropWeight;
    private final MaterialTier tier;
    private final Color color;

    CraftingCurrency(String itemId, int defaultPartsCost, float dropWeight, MaterialTier tier, Color color) {
        this.itemId = itemId;
        this.defaultPartsCost = defaultPartsCost;
        this.dropWeight = dropWeight;
        this.tier = tier;
        this.color = color;
    }

    public static CraftingCurrency byItemId(String itemId) {
        for (CraftingCurrency currency : values()) {
            if (currency.itemId.equals(itemId)) {
                return currency;
            }
        }
        return null;
    }

    public String itemId() {
        return itemId;
    }

    public float dropWeight() {
        return dropWeight;
    }

    public MaterialTier tier() {
        return tier;
    }

    public Color color() {
        return color;
    }

    public boolean synthesisable() {
        return defaultPartsCost > 0;
    }

    public int partsCost() {
        return ItemCraftingCosts.cost(itemId, defaultPartsCost);
    }
}
