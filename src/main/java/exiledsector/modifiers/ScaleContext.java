package exiledsector.modifiers;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.EnumMap;
import java.util.Map;

public record ScaleContext(HullSize hullSize, int tier) {

    public static final ScaleContext NONE = new ScaleContext(null, TierRange.NO_TIER);
    private static final Map<HullSize, ScaleContext> BY_HULL_SIZE = byHullSize();

    public static ScaleContext ofHullSize(HullSize hullSize) {
        return hullSize == null ? NONE : BY_HULL_SIZE.get(hullSize);
    }

    public static ScaleContext ofTier(int tier) {
        return tier == TierRange.NO_TIER ? NONE : new ScaleContext(null, tier);
    }

    public static ScaleContext of(HullSize hullSize, int tier) {
        return tier == TierRange.NO_TIER ? ofHullSize(hullSize) : new ScaleContext(hullSize, tier);
    }

    private static Map<HullSize, ScaleContext> byHullSize() {
        Map<HullSize, ScaleContext> contexts = new EnumMap<>(HullSize.class);
        for (HullSize hullSize : HullSize.values()) {
            contexts.put(hullSize, new ScaleContext(hullSize, TierRange.NO_TIER));
        }
        return contexts;
    }
}
