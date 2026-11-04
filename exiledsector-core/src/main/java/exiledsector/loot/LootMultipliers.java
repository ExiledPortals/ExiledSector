package exiledsector.loot;

import exiledsector.items.ItemChances;

public record LootMultipliers(ItemChances itemChances, float feedstockMult, float craftingItemMult) {

    public static final LootMultipliers NONE = new LootMultipliers(ItemChances.BASE, 1f, 1f);

    public static LootMultipliers of(float mult) {
        return new LootMultipliers(ItemChances.of(mult), mult, mult);
    }
}
