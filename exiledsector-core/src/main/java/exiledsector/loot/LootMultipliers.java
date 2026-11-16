package exiledsector.loot;

import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemChances;

public record LootMultipliers(ItemChances itemChances, float feedstockMult, float craftingItemMult, float craftingBandShift) {

    public static final LootMultipliers NONE = new LootMultipliers(ItemChances.BASE, 1f, 1f);

    public LootMultipliers(ItemChances itemChances, float feedstockMult, float craftingItemMult) {
        this(itemChances, feedstockMult, craftingItemMult, CraftingDrops.NO_BAND_SHIFT);
    }

    public static LootMultipliers of(float mult) {
        return new LootMultipliers(ItemChances.of(mult), mult, mult);
    }

    public LootMultipliers withItemChances(ItemChances chances) {
        return new LootMultipliers(chances, feedstockMult, craftingItemMult, craftingBandShift);
    }

    public LootMultipliers withCraftingItemMult(float mult) {
        return new LootMultipliers(itemChances, feedstockMult, mult, craftingBandShift);
    }

    public LootMultipliers withCraftingBandShift(float bandShift) {
        return new LootMultipliers(itemChances, feedstockMult, craftingItemMult, bandShift);
    }
}
