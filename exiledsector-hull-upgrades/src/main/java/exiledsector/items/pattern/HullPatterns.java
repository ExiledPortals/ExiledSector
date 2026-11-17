package exiledsector.items.pattern;

import com.fs.starfarer.api.Global;
import exiledsector.persistence.SaveData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class HullPatterns {

    static final String SAVE_KEY = "exiledSector_hullPatterns";
    private static final HullPatterns NONE = new HullPatterns(Map.of());
    private static final AtomicInteger REVISION = new AtomicInteger();

    private final Map<String, HullPattern> patternsByHullType;

    HullPatterns(Map<String, HullPattern> patternsByHullType) {
        this.patternsByHullType = patternsByHullType;
    }

    public static HullPatterns get() {
        if (Global.getSector() == null) {
            return NONE;
        }
        Map<String, HullPattern> saved = SaveData.findOrNull(SAVE_KEY, Map.class);
        return saved == null ? NONE : new HullPatterns(saved);
    }

    public static int revision() {
        return REVISION.get();
    }

    public static void markChanged() {
        REVISION.incrementAndGet();
    }

    public boolean isEmpty() {
        return patternsByHullType.isEmpty();
    }

    public Collection<HullPattern> all() {
        return List.copyOf(patternsByHullType.values());
    }

    public HullPattern forHullType(String hullType) {
        return hullType == null ? null : patternsByHullType.get(hullType);
    }

    public HullPattern ofPatternShip(String shipId) {
        if (shipId == null) {
            return null;
        }
        for (HullPattern pattern : patternsByHullType.values()) {
            if (shipId.equals(pattern.patternShipId())) {
                return pattern;
            }
        }
        return null;
    }

    public HullPattern ofFollower(String shipId) {
        if (shipId == null) {
            return null;
        }
        for (HullPattern pattern : patternsByHullType.values()) {
            if (pattern.isFollower(shipId)) {
                return pattern;
            }
        }
        return null;
    }

    static HullPattern create(String hullType, String patternShipId) {
        Map<String, HullPattern> saved = SaveData.getOrCreate(SAVE_KEY, LinkedHashMap::new);
        return new HullPatterns(saved).createIn(hullType, patternShipId);
    }

    HullPattern createIn(String hullType, String patternShipId) {
        if (hullType == null || patternShipId == null || patternsByHullType.containsKey(hullType) || ofPatternShip(patternShipId) != null
                || ofFollower(patternShipId) != null) {
            return null;
        }
        HullPattern pattern = new HullPattern(hullType, patternShipId);
        patternsByHullType.put(hullType, pattern);
        markChanged();
        return pattern;
    }

    public boolean delete(HullPattern pattern) {
        if (pattern == null || patternsByHullType.get(pattern.hullType()) != pattern) {
            return false;
        }
        patternsByHullType.remove(pattern.hullType());
        markChanged();
        return true;
    }
}
