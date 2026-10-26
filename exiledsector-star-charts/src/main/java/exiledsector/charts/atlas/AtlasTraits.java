package exiledsector.charts.atlas;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AtlasTraits(AtlasCell cell, float distance, String regionId, String factionId, String territoryId, String layoutId,
                          Set<String> neighbourTerritoryIds, List<String> neighbourFactionIds, String specialId, String infestationId) {

    public AtlasTraits {
        neighbourTerritoryIds = Set.copyOf(neighbourTerritoryIds);
        neighbourFactionIds = List.copyOf(neighbourFactionIds);
    }

    public boolean bordersOtherTerritory() {
        return neighbourTerritoryIds.stream().anyMatch(neighbour -> !Objects.equals(neighbour, territoryId));
    }
}
