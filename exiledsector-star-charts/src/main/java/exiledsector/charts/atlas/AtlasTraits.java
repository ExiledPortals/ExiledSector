package exiledsector.charts.atlas;

import java.util.Objects;
import java.util.Set;

public record AtlasTraits(AtlasCell cell, float distance, String regionId, String factionId, String territoryId, String layoutId,
                          Set<String> neighbourTerritoryIds) {

    public AtlasTraits {
        neighbourTerritoryIds = Set.copyOf(neighbourTerritoryIds);
    }

    public boolean bordersOtherTerritory() {
        return neighbourTerritoryIds.stream().anyMatch(neighbour -> !Objects.equals(neighbour, territoryId));
    }
}
