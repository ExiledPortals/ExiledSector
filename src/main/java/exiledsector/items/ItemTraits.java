package exiledsector.items;

import java.util.List;

public record ItemTraits(int tier, int quality, CorruptionOutcome corruption, List<RolledModifier> implicits) {

    public ItemTraits {
        implicits = implicits == null ? null : List.copyOf(implicits);
    }

    public static ItemTraits ofTier(int tier) {
        return new ItemTraits(tier, 0, null, null);
    }
}
