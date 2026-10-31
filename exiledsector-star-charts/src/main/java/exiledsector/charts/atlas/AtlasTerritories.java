package exiledsector.charts.atlas;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

final class AtlasTerritories {

    static final float TERRITORY_SPACING = 16f;
    private static final long TERRITORY_POINT_SALT = 0x54455252505443L;
    private static final long TERRITORY_PICK_SALT = 0x544552525043L;
    private static final long FACTION_SALT = 0x46414354L;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasPositions positions;
    private final AtlasMemory memory;
    private final JitteredGrid territoryPoints;

    private record SeedPoint(int gridX, int gridY) {
    }

    AtlasTerritories(long seed, AtlasRules rules, AtlasPositions positions, AtlasMemory memory) {
        this.seed = seed;
        this.rules = rules;
        this.positions = positions;
        this.memory = memory;
        this.territoryPoints = new JitteredGrid(seed, TERRITORY_POINT_SALT);
    }

    String territoryAt(AtlasCell cell) {
        return territoryFor(territoryPoint(cell));
    }

    String factionAt(AtlasCell cell, AtlasRegion region) {
        SeedPoint point = territoryPoint(cell);
        String territory = territoryFor(point);
        return keepsTerritory(region, territory) ? territory : regionFaction(region, point);
    }

    void recordFactions(AtlasCell cell, AtlasRegion region, BiConsumer<String, String> recorder) {
        SeedPoint point = territoryPoint(cell);
        String territory = territoryFor(point);
        if (territory != null) {
            recorder.accept(territoryKey(point), territory);
        }
        if (!keepsTerritory(region, territory)) {
            String faction = regionFaction(region, point);
            if (faction != null) {
                recorder.accept(regionKey(region, point), faction);
            }
        }
    }

    private static boolean keepsTerritory(AtlasRegion region, String territory) {
        return region.factionWeights().isEmpty() || territory != null && holds(region.factionWeights(), territory);
    }

    private String regionFaction(AtlasRegion region, SeedPoint point) {
        return recordedOr(regionKey(region, point), region.factionWeights(),
                () -> AtlasHash.race(seed, FACTION_SALT ^ AtlasHash.idSalt(region.id()), point.gridX(), point.gridY(), region.factionWeights()));
    }

    private String recordedOr(String key, Map<String, Float> weights, Supplier<String> rolled) {
        String recorded = memory.recordedFaction(key);
        return recorded != null && holds(weights, recorded) ? recorded : rolled.get();
    }

    private static boolean holds(Map<String, Float> weights, String factionId) {
        return weights.getOrDefault(factionId, 0f) > 0f;
    }

    private static String territoryKey(SeedPoint point) {
        return point.gridX() + "," + point.gridY();
    }

    private static String regionKey(AtlasRegion region, SeedPoint point) {
        return region.id() + "@" + territoryKey(point);
    }

    Set<String> laneTerritories(List<AtlasCell> lanes) {
        Set<String> territories = new HashSet<>();
        for (AtlasCell neighbour : lanes) {
            String territory = territoryAt(neighbour);
            if (territory != null) {
                territories.add(territory);
            }
        }
        return territories;
    }

    private SeedPoint territoryPoint(AtlasCell cell) {
        float px = positions.mapX(cell) / TERRITORY_SPACING;
        float py = positions.mapY(cell) / TERRITORY_SPACING;
        int baseX = (int) Math.floor(px);
        int baseY = (int) Math.floor(py);
        SeedPoint nearest = null;
        float nearestDistance = Float.POSITIVE_INFINITY;
        for (int gridX = baseX - 2; gridX <= baseX + 2; gridX++) {
            for (int gridY = baseY - 2; gridY <= baseY + 2; gridY++) {
                if (Math.abs(gridX - baseX) == 2 && Math.abs(gridY - baseY) == 2) {
                    continue;
                }
                float sx = territoryPoints.x(gridX, gridY);
                float sy = territoryPoints.y(gridX, gridY);
                float distance = AtlasPositions.squared(sx - px, sy - py);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = new SeedPoint(gridX, gridY);
                }
            }
        }
        return nearest;
    }

    Map<String, Float> factionPool(AtlasRegion region) {
        return region.factionWeights().isEmpty() ? territoryPool() : region.factionWeights();
    }

    private Map<String, Float> territoryPool() {
        Map<String, Float> normalPool = rules.normalRegion().factionWeights();
        return normalPool.isEmpty() ? rules.factionWeights() : normalPool;
    }

    private String territoryFor(SeedPoint point) {
        Map<String, Float> pool = territoryPool();
        return recordedOr(territoryKey(point), pool, () -> AtlasHash.race(seed, TERRITORY_PICK_SALT, point.gridX(), point.gridY(), pool));
    }
}
