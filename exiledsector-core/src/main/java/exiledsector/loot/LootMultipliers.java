package exiledsector.loot;

import exiledsector.items.drop.CraftingDrops;
import exiledsector.items.drop.ItemChances;

public record LootMultipliers(ItemChances chances, float tierCraftingRarity) {

    public static final LootMultipliers NONE = of(1f);

    public LootMultipliers {
        chances = chances == null ? ItemChances.BASE : chances;
    }

    public static LootMultipliers of(float share) {
        return new LootMultipliers(ItemChances.atShare(share), CraftingDrops.BASE_RARITY);
    }

    public static LootMultipliers atTierCraftingRarity(float tierCraftingRarity) {
        return new LootMultipliers(ItemChances.BASE, tierCraftingRarity);
    }

    public float quantity(DropType type) {
        return chances.quantity(type);
    }

    public float rarity(DropType type) {
        return chances.rarity(type);
    }

    public float craftingRarity(DropType craftingSubcategory) {
        return tierCraftingRarity * chances.rarity(craftingSubcategory);
    }

    public LootMultipliers withChances(ItemChances chances) {
        return new LootMultipliers(chances, tierCraftingRarity);
    }
}
