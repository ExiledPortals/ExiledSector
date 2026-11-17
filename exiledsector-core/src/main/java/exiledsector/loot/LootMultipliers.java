package exiledsector.loot;

import exiledsector.items.drop.CraftingDrops;
import exiledsector.items.drop.ItemChances;

public record LootMultipliers(ItemChances chances, float tierBandShift) {

    public static final LootMultipliers NONE = of(1f);

    public LootMultipliers {
        chances = chances == null ? ItemChances.BASE : chances;
    }

    public static LootMultipliers of(float share) {
        return new LootMultipliers(ItemChances.atShare(share), CraftingDrops.NO_BAND_SHIFT);
    }

    public static LootMultipliers atTierBandShift(float tierBandShift) {
        return new LootMultipliers(ItemChances.BASE, tierBandShift);
    }

    public float quantity(DropType type) {
        return chances.quantity(type);
    }

    public float rarity(DropType type) {
        return chances.rarity(type);
    }

    public float craftingBandShift(DropType craftingSubcategory) {
        return tierBandShift * chances.rarity(craftingSubcategory);
    }

    public LootMultipliers withChances(ItemChances chances) {
        return new LootMultipliers(chances, tierBandShift);
    }
}
