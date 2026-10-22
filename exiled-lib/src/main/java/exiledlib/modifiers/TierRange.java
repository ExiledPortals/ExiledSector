package exiledlib.modifiers;

public record TierRange(int minTier, int maxTier) {

    public static final int NO_TIER = 0;
    public static final int LOWEST_TIER = 1;
    public static final TierRange ANY = new TierRange(LOWEST_TIER, Integer.MAX_VALUE);

    public TierRange {
        if (minTier < LOWEST_TIER) {
            throw new IllegalArgumentException("the minimum tier must be " + LOWEST_TIER + " or more, not " + minTier);
        }
        if (maxTier < minTier) {
            throw new IllegalArgumentException("the maximum tier " + maxTier + " is below the minimum tier " + minTier);
        }
    }

    public static TierRange atLeast(int minTier) {
        return new TierRange(minTier, Integer.MAX_VALUE);
    }

    public static TierRange between(int minTier, int maxTier) {
        return new TierRange(minTier, maxTier);
    }

    public boolean isAny() {
        return equals(ANY);
    }

    public boolean allows(int tier) {
        return tier == NO_TIER || (tier >= minTier && tier <= maxTier);
    }
}
