package exiledsector.items.modifier;

import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemTraits;
import exiledsector.items.ItemType;
import exiledsector.items.ItemTypes;
import exiledsector.modifiers.TierRange;

public record ItemRoll(int tier, float rarity, int modifierCount, Limits limits) {

    public enum Limits {
        AFFIX,
        DROP;

        AffixLayout.Limits of(ItemType type) {
            return this == DROP ? AffixLayout.dropLimits(type) : AffixLayout.limits(type);
        }
    }

    private static final int TIERLESS = Integer.MIN_VALUE;
    private static final float BASE_RARITY = 1f;
    private static final int RANDOM_MODIFIER_COUNT = -1;
    public static final ItemRoll PLAIN = new ItemRoll(TIERLESS, BASE_RARITY, RANDOM_MODIFIER_COUNT, Limits.AFFIX);

    public static ItemRoll atTier(int tier) {
        return PLAIN.withTier(tier);
    }

    public static ItemRoll withModifierCount(int modifierCount) {
        return new ItemRoll(TIERLESS, BASE_RARITY, modifierCount, Limits.AFFIX);
    }

    public static ItemRoll dropAtRarity(float rarity) {
        return new ItemRoll(TIERLESS, rarity, RANDOM_MODIFIER_COUNT, Limits.DROP);
    }

    public ItemRoll withTier(int rolledTier) {
        return new ItemRoll(rolledTier, rarity, modifierCount, limits);
    }

    public ItemRoll withRarity(float rolledRarity) {
        return new ItemRoll(tier, rolledRarity, modifierCount, limits);
    }

    public boolean hasTier() {
        return tier != TIERLESS;
    }

    public boolean fixesModifierCount() {
        return modifierCount != RANDOM_MODIFIER_COUNT;
    }

    int affixTier() {
        return hasTier() ? tier : TierRange.NO_TIER;
    }

    public ItemRoll startingAtLowestTierFor(ItemDefinition definition) {
        boolean tierlessTieredItem = ItemTypes.tiered(definition.type()) && !hasTier() && !fixesModifierCount();
        return tierlessTieredItem ? withTier(ItemTraits.LOWEST_TIER) : this;
    }

    public ItemRoll resolvedFor(ItemDefinition definition) {
        int resolvedTier = hasTier() ? ItemTypes.rolledTier(definition, tier) : TIERLESS;
        if (ItemTypes.tiered(definition.type())) {
            return new ItemRoll(resolvedTier, BASE_RARITY, RANDOM_MODIFIER_COUNT, Limits.AFFIX);
        }
        return withTier(resolvedTier);
    }
}
