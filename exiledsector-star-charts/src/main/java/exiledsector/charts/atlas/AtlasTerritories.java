package exiledsector.charts.atlas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AtlasTerritories {

    static final float TERRITORY_SPACING = 16f;
    private static final long TERRITORY_POINT_SALT = 0x54455252505443L;
    private static final long TERRITORY_PICK_SALT = 0x544552525043L;
    private static final long FACTION_SALT = 0x46414354L;
    private static final long AXIS_SALT = 0x59L;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasPositions positions;

    private record SeedPoint(int gridX, int gridY) {
    }

    AtlasTerritories(long seed, AtlasRules rules, AtlasPositions positions) {
        this.seed = seed;
        this.rules = rules;
        this.positions = positions;
    }

    String territoryAt(AtlasCell cell) {
        return territoryFor(territoryPoint(cell));
    }

    String factionAt(AtlasCell cell, AtlasRegion region) {
        SeedPoint point = territoryPoint(cell);
        String territory = territoryFor(point);
        if (region.factionWeights().isEmpty()) {
            return territory;
        }
        if (territory != null && region.factionWeights().getOrDefault(territory, 0f) > 0f) {
            return territory;
        }
        return AtlasHash.race(seed, FACTION_SALT ^ AtlasHash.idSalt(region.id()), point.gridX(), point.gridY(), region.factionWeights());
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
                float sx = gridX + (float) AtlasHash.unit(AtlasHash.of(seed, TERRITORY_POINT_SALT, gridX, gridY));
                float sy = gridY + (float) AtlasHash.unit(AtlasHash.of(seed, TERRITORY_POINT_SALT ^ AXIS_SALT, gridX, gridY));
                float distance = AtlasPositions.squared(sx - px, sy - py);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = new SeedPoint(gridX, gridY);
                }
            }
        }
        return nearest;
    }

    private String territoryFor(SeedPoint point) {
        return AtlasHash.race(seed, TERRITORY_PICK_SALT, point.gridX(), point.gridY(), rules.factionWeights());
    }
}
