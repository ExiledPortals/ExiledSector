package exiledsector.loot;

import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemQuantity;

public record LootMultipliers(ItemChances chances, float feedstockMult, ItemQuantity craftingItems, float tierBandShift) {

    public static final LootMultipliers NONE = of(1f);

    public LootMultipliers {
        chances = chances == null ? ItemChances.BASE : chances;
        craftingItems = craftingItems == null ? ItemQuantity.BASE : craftingItems;
    }

    public static LootMultipliers of(float mult) {
        return new LootMultipliers(ItemChances.of(mult), mult, ItemQuantity.atShare(mult), CraftingDrops.NO_BAND_SHIFT);
    }

    public float craftingBandShift() {
        return tierBandShift * chances.rarity();
    }

    public LootMultipliers withChances(ItemChances chances) {
        return new LootMultipliers(chances, feedstockMult, craftingItems, tierBandShift);
    }
}
