package exiledsector.loot;

import exiledlib.items.ItemChances;

public record LootModifiers(ItemChances itemChances, float feedstockMult, float currencyMult) {

    public static final LootModifiers NONE = new LootModifiers(ItemChances.BASE, 1f, 1f);
}
