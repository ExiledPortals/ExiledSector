package exiledsector.modifiers;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.EnumMap;
import java.util.Map;

public record ScaleContext(HullSize hullSize, int tier) {

    public static final ScaleContext NONE = new ScaleContext(null, TierRange.NO_TIER);
    private static final Map<HullSize, ScaleContext> BY_HULL_SIZE = byHullSize();
    private static final int CACHED_TIERS = 32;
    private static final ScaleContext[] BY_TIER = byTier();

    public static ScaleContext ofHullSize(HullSize hullSize) {
        return hullSize == null ? NONE : BY_HULL_SIZE.get(hullSize);
    }

    public static ScaleContext ofTier(int tier) {
        if (tier == TierRange.NO_TIER) {
            return NONE;
        }
        return tier > 0 && tier <= CACHED_TIERS ? BY_TIER[tier] : new ScaleContext(null, tier);
    }

    public static ScaleContext of(HullSize hullSize, int tier) {
        if (tier == TierRange.NO_TIER) {
            return ofHullSize(hullSize);
        }
        return hullSize == null ? ofTier(tier) : new ScaleContext(hullSize, tier);
    }

    private static ScaleContext[] byTier() {
        ScaleContext[] contexts = new ScaleContext[CACHED_TIERS + 1];
        for (int tier = 1; tier <= CACHED_TIERS; tier++) {
            contexts[tier] = new ScaleContext(null, tier);
        }
        return contexts;
    }

    private static Map<HullSize, ScaleContext> byHullSize() {
        Map<HullSize, ScaleContext> contexts = new EnumMap<>(HullSize.class);
        for (HullSize hullSize : HullSize.values()) {
            contexts.put(hullSize, new ScaleContext(hullSize, TierRange.NO_TIER));
        }
        return contexts;
    }
}
