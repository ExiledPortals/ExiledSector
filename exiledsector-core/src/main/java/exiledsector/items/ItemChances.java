package exiledsector.items;

import java.util.EnumMap;
import java.util.Map;

public record ItemChances(ItemQuantity quantity, Map<ItemKind, Float> targeted, float rarity) {

    public static final ItemChances BASE = new ItemChances(ItemQuantity.BASE, Map.of(), 1f);

    public ItemChances {
        quantity = quantity == null ? ItemQuantity.BASE : quantity;
        targeted = targeted == null ? Map.of() : Map.copyOf(targeted);
        rarity = Math.max(0f, rarity);
    }

    public static ItemChances of(float general) {
        return general == 1f ? BASE : new ItemChances(ItemQuantity.atShare(general), Map.of(), 1f);
    }

    public static ItemChances of(ItemQuantity quantity) {
        return new ItemChances(quantity, Map.of(), 1f);
    }

    public float general() {
        return quantity.mult();
    }

    public float generalPlusPercent(float extraIncreasedPercent) {
        return quantity.plusPercent(extraIncreasedPercent).mult();
    }

    public ItemChances plusPercent(float extraIncreasedPercent) {
        ItemQuantity increased = quantity.plusPercent(extraIncreasedPercent);
        return increased == quantity ? this : new ItemChances(increased, targeted, rarity);
    }

    public ItemChances withRarity(float rarityMult) {
        return rarityMult == rarity ? this : new ItemChances(quantity, targeted, rarityMult);
    }

    public ItemChances withTargeted(Map<ItemKind, Float> extraShares) {
        return new ItemChances(quantity, extraShares, rarity);
    }

    public ItemChances scaled(float factor) {
        if (factor == 1f) {
            return this;
        }
        Map<ItemKind, Float> scaledTargeted = new EnumMap<>(ItemKind.class);
        targeted.forEach((kind, extraShare) -> scaledTargeted.put(kind, extraShare * factor));
        return new ItemChances(quantity.scaled(factor), scaledTargeted, rarity);
    }

    public float targeted(ItemKind kind) {
        Float extraShare = targeted.get(kind);
        return extraShare == null ? 0f : Math.max(0f, extraShare);
    }
}
