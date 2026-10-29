package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

final class AtlasBiomeAnomalies {

    static final long PICK_SALT = 0x42494f4d45504bL;
    static final int POCKET_WINDOW = (int) Math.ceil(AtlasBiomes.SITE_REACH + AtlasPositions.JITTER_REACH * Math.sqrt(2)) + 1;
    private static final int CACHE_LIMIT = 1024;

    private final long seed;
    private final List<AtlasAnomalyCandidate> candidates;
    private final Function<AtlasCell, AtlasBiome> biomeOf;
    private final Predicate<AtlasCell> canHold;
    private final Map<Long, Map<String, AtlasCell>> picks = AtlasPositions.boundedCache(CACHE_LIMIT);

    AtlasBiomeAnomalies(long seed, List<AtlasAnomalyCandidate> candidates, Function<AtlasCell, AtlasBiome> biomeOf, Predicate<AtlasCell> canHold) {
        this.seed = seed;
        this.candidates = candidates.stream().filter(AtlasAnomalyCandidate::placedPerBiome).toList();
        this.biomeOf = biomeOf;
        this.canHold = canHold;
    }

    List<String> at(AtlasCell cell) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        picksFor(biomeOf.apply(cell)).forEach((anomalyId, picked) -> {
            if (picked.equals(cell)) {
                ids.add(anomalyId);
            }
        });
        return ids;
    }

    AtlasCell pick(AtlasBiome biome, String anomalyId) {
        return candidates.isEmpty() ? null : picksFor(biome).get(anomalyId);
    }

    private Map<String, AtlasCell> picksFor(AtlasBiome biome) {
        Map<String, AtlasCell> cached = picks.get(biome.siteKey());
        if (cached != null) {
            return cached;
        }
        List<AtlasAnomalyCandidate> wanted = candidates.stream().filter(candidate -> candidate.biomes().contains(biome.regionId())).toList();
        Map<String, AtlasCell> picked = wanted.isEmpty() ? Map.of() : choose(biome, wanted);
        picks.put(biome.siteKey(), picked);
        return picked;
    }

    private Map<String, AtlasCell> choose(AtlasBiome biome, List<AtlasAnomalyCandidate> wanted) {
        Map<String, AtlasCell> best = new HashMap<>();
        Map<String, Double> bestRolls = new HashMap<>();
        int centreX = Math.round(biome.centreX());
        int centreY = Math.round(biome.centreY());
        for (int gridX = centreX - POCKET_WINDOW; gridX <= centreX + POCKET_WINDOW; gridX++) {
            for (int gridY = centreY - POCKET_WINDOW; gridY <= centreY + POCKET_WINDOW; gridY++) {
                AtlasCell cell = new AtlasCell(gridX, gridY);
                if (biomeOf.apply(cell).siteKey() != biome.siteKey() || !canHold.test(cell)) {
                    continue;
                }
                for (AtlasAnomalyCandidate candidate : wanted) {
                    double roll = AtlasHash.unit(AtlasHash.of(seed, PICK_SALT, candidate.id(), gridX, gridY));
                    Double bestRoll = bestRolls.get(candidate.id());
                    if (bestRoll == null || roll < bestRoll) {
                        bestRolls.put(candidate.id(), roll);
                        best.put(candidate.id(), cell);
                    }
                }
            }
        }
        Map<String, AtlasCell> ordered = new LinkedHashMap<>();
        for (AtlasAnomalyCandidate candidate : wanted) {
            if (best.containsKey(candidate.id())) {
                ordered.put(candidate.id(), best.get(candidate.id()));
            }
        }
        return Collections.unmodifiableMap(ordered);
    }
}
