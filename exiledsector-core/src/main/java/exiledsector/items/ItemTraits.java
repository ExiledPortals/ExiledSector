package exiledsector.items;

import java.util.List;

public record ItemTraits(int tier, int quantity, CorruptionOutcome corruption, List<RolledModifier> implicits, boolean mirrored) {

    public static final int LOWEST_TIER = 1;
    public static final int HIGHEST_DROP_TIER = 15;
    public static final int HIGHEST_TIER = HIGHEST_DROP_TIER + 1;

    public static int clampTier(int tier) {
        return Math.max(LOWEST_TIER, Math.min(HIGHEST_TIER, tier));
    }

    public ItemTraits {
        implicits = implicits == null ? null : List.copyOf(implicits);
    }

    public ItemTraits(int tier, int quantity, CorruptionOutcome corruption, List<RolledModifier> implicits) {
        this(tier, quantity, corruption, implicits, false);
    }

    public static ItemTraits ofTier(int tier) {
        return new ItemTraits(tier, 0, null, null);
    }
}
