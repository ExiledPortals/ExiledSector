package exiledsector.modifiers;

import java.util.Arrays;
import java.util.List;

public enum TierGroup {
    LOW("low", TierRange.between(1, 5)),
    MID("mid", TierRange.between(6, 10)),
    HIGH("high", TierRange.atLeast(11));

    private static final List<TierGroup> ORDER = List.of(values());

    private final String id;
    private final TierRange tiers;

    TierGroup(String id, TierRange tiers) {
        this.id = id;
        this.tiers = tiers;
    }

    public String id() {
        return id;
    }

    public TierRange tiers() {
        return tiers;
    }

    public boolean contains(int tier) {
        return tier != TierRange.NO_TIER && tiers.allows(tier);
    }

    public static TierGroup byIdOrNull(String groupId) {
        for (TierGroup group : ORDER) {
            if (group.id.equals(groupId)) {
                return group;
            }
        }
        return null;
    }

    public static TierGroup forTier(int tier) {
        for (TierGroup group : ORDER) {
            if (group.contains(tier)) {
                return group;
            }
        }
        return null;
    }

    public static List<TierGroup> fromUpwards(TierGroup lowestGroup) {
        return ORDER.subList(lowestGroup.ordinal(), ORDER.size());
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(TierGroup::id).toList();
    }
}
