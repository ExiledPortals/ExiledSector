package exiledsector.charts.atlas;

import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.anomaly.skirmish.SkirmishFactions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class TerritoryFactions {

    static final float INDEPENDENT_WEIGHT = 5f;
    static final float MERCENARY_WEIGHT = 3f;
    private static final Set<String> UNCLAIMED_FACTIONS = Set.of(Factions.INDEPENDENT, Factions.MERCENARY);
    private static final long UNCLAIMED_SALT = 0x554e434c4149L;
    private static final String TERRITORY_SEPARATOR = "|";

    private TerritoryFactions() {
    }

    public static boolean canHoldTerritory(String factionId) {
        return SkirmishFactions.takesSides(factionId) && !UNCLAIMED_FACTIONS.contains(factionId) && !Factions.PLAYER.equals(factionId);
    }

    public static String territoryId(String factionId, AtlasCell commandCentre) {
        return factionId + TERRITORY_SEPARATOR + commandCentre.pack();
    }

    public static String factionOfTerritory(String territoryId) {
        if (territoryId == null) {
            return null;
        }
        int split = territoryId.lastIndexOf(TERRITORY_SEPARATOR);
        return split < 0 ? territoryId : territoryId.substring(0, split);
    }

    public static Map<String, Float> territoryWeights(AtlasRules rules, AtlasRegion region) {
        return capable(region.factionWeights().isEmpty() ? normalPool(rules) : region.factionWeights());
    }

    public static Map<String, Float> boardFactions(AtlasRules rules) {
        Map<String, Float> factions = new LinkedHashMap<>();
        addCapable(factions, normalPool(rules), rules);
        for (AtlasRegion region : rules.regions()) {
            addCapable(factions, region.factionWeights(), rules);
        }
        return factions;
    }

    static String unclaimedFaction(long seed, AtlasRules rules, AtlasRegion region, AtlasCell cell) {
        Map<String, Float> regionPool = region.factionWeights();
        if (!regionPool.isEmpty() && capable(regionPool).isEmpty()) {
            return AtlasHash.race(seed, UNCLAIMED_SALT ^ AtlasHash.idSalt(region.id()), cell.gridX(), cell.gridY(), regionPool);
        }
        Map<String, Float> unclaimed = new LinkedHashMap<>();
        unclaimed.put(Factions.INDEPENDENT, rules.factionWeights().getOrDefault(Factions.INDEPENDENT, INDEPENDENT_WEIGHT));
        unclaimed.put(Factions.MERCENARY, rules.factionWeights().getOrDefault(Factions.MERCENARY, MERCENARY_WEIGHT));
        return AtlasHash.race(seed, UNCLAIMED_SALT, cell.gridX(), cell.gridY(), unclaimed);
    }

    private static Map<String, Float> normalPool(AtlasRules rules) {
        Map<String, Float> normalPool = rules.normalRegion().factionWeights();
        return normalPool.isEmpty() ? rules.factionWeights() : normalPool;
    }

    private static Map<String, Float> capable(Map<String, Float> pool) {
        Map<String, Float> capable = new LinkedHashMap<>();
        pool.forEach((factionId, weight) -> {
            if (weight > 0f && canHoldTerritory(factionId)) {
                capable.put(factionId, weight);
            }
        });
        return capable;
    }

    private static void addCapable(Map<String, Float> factions, Map<String, Float> pool, AtlasRules rules) {
        for (String factionId : capable(pool).keySet()) {
            float weight = rules.factionWeights().getOrDefault(factionId, 1f);
            if (weight > 0f) {
                factions.putIfAbsent(factionId, weight);
            }
        }
    }
}
