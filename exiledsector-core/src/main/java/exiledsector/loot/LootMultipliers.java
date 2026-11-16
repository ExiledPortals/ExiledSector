package exiledsector.loot;

import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemChances;

public record LootMultipliers(ItemChances chances, float feedstockMult, float craftingItemMult, float tierBandShift) {

    public static final LootMultipliers NONE = of(1f);

    public static LootMultipliers of(float mult) {
        return new LootMultipliers(ItemChances.of(mult), mult, mult, CraftingDrops.NO_BAND_SHIFT);
    }

    public float craftingBandShift() {
        return tierBandShift * chances.rarity();
    }

    public LootMultipliers withChances(ItemChances chances) {
        return new LootMultipliers(chances, feedstockMult, craftingItemMult, tierBandShift);
    }

    public LootMultipliers withCraftingItemMult(float mult) {
        return new LootMultipliers(chances, feedstockMult, mult, tierBandShift);
    }
}
