package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasGenerator {

    public static final int VERSION = 1;
    public static final float DISPLAY_JITTER_SHARE = AtlasPositions.DISPLAY_JITTER_SHARE;
    static final float LONGEST_LANE = (float) (3.0 * Math.sqrt(2.0));
    static final int LANE_WINDOW = 5;
    static final Set<String> THREAT_ONLY = Set.of("threat");
    private static final int CACHE_LIMIT_CELLS = 65536;
    private static final long VOID_SALT = 0x564f4944L;
    private static final long LAYOUT_SALT = 0x4c41594fL;
    private static final long ANOMALY_SALT = 0x455850414eL;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasUnlocks unlocks;
    private final AtlasPositions positions;
    private final AtlasSpecialPlacer specials;
    private final AtlasBiomes biomes;
    private final AtlasTerritories territories;
    private final AtlasTerrain terrain;
    private final Map<Long, List<AtlasCell>> laneCache = AtlasPositions.boundedCache(CACHE_LIMIT_CELLS);
    private final Map<Long, Boolean> voidCache = AtlasPositions.boundedCache(CACHE_LIMIT_CELLS);

    public AtlasGenerator(long seed, AtlasRules rules, AtlasUnlocks unlocks, AtlasMemory memory) {
        this.seed = seed;
        this.rules = rules;
        this.unlocks = unlocks;
        positions = new AtlasPositions(seed);
        specials = new AtlasSpecialPlacer(seed, rules, unlocks, memory, positions, this::isVoid);
        biomes = new AtlasBiomes(seed, rules, unlocks, memory, positions);
        territories = new AtlasTerritories(seed, rules, positions, memory);
        terrain = new AtlasTerrain(seed, rules, biomes, positions, specials.fixedCells());
    }

    public long seed() {
        return seed;
    }

    public AtlasRules rules() {
        return rules;
    }

    public Set<String> unlockFlags() {
        return unlocks.unlockedFlags();
    }

    public static long chunkKey(int chunkX, int chunkY) {
        return new AtlasCell(chunkX, chunkY).pack();
    }

    public AtlasNode node(AtlasCell cell) {
        return node(cell, specials.specialsAround(cell.chunkX(), cell.chunkY()));
    }

    public List<AtlasNode> chunk(int chunkX, int chunkY) {
        Map<Long, AtlasSpecialPlacer.PlacedSpecial> placed = specials.specialsAround(chunkX, chunkY);
        List<AtlasNode> nodes = new ArrayList<>(AtlasCell.CHUNK_SIZE * AtlasCell.CHUNK_SIZE);
        for (AtlasCell cell : AtlasSpecialPlacer.cellsOf(chunkX, chunkY)) {
            nodes.add(node(cell, placed));
        }
        return nodes;
    }

    public boolean isVoid(AtlasCell cell) {
        Boolean cached = voidCache.get(cell.pack());
        if (cached != null) {
            return cached;
        }
        boolean empty = computeVoid(cell);
        voidCache.put(cell.pack(), empty);
        return empty;
    }

    public float mapX(AtlasCell cell) {
        return positions.mapX(cell);
    }

    public float mapY(AtlasCell cell) {
        return positions.mapY(cell);
    }

    public float displayX(AtlasCell cell) {
        return positions.displayX(cell);
    }

    public float displayY(AtlasCell cell) {
        return positions.displayY(cell);
    }

    public boolean cloudAt(float displayX, float displayY) {
        return terrain.cloudAt(displayX, displayY);
    }

    public boolean laneClear(AtlasCell from, AtlasCell to) {
        return terrain.laneClear(displayX(from), displayY(from), displayX(to), displayY(to));
    }

    public List<AtlasCell> lanes(AtlasCell cell) {
        if (isVoid(cell)) {
            return List.of();
        }
        return laneCache.computeIfAbsent(cell.pack(), packed -> relativeNeighbours(cell));
    }

    public AtlasBiome biomeOf(AtlasCell cell) {
        return biomes.biomeOf(cell);
    }

    public List<AtlasBiome> biomesAround(int chunkX, int chunkY) {
        return biomes.biomesAround(chunkX, chunkY);
    }

    public List<AtlasBiome> biomeContenders(AtlasCell cell) {
        return biomes.contenders(cell, AtlasTerrain.RECORD_REACH);
    }

    public AtlasSpecial specialAt(AtlasCell cell) {
        return specials.specialAt(cell);
    }

    public AtlasCell infestationSource(AtlasCell cell) {
        AtlasSpecialPlacer.PlacedSpecial source = infestationOf(cell, regionOf(cell), specials.specialsAround(cell.chunkX(), cell.chunkY()));
        return source == null ? null : source.cell();
    }

    String territoryAt(AtlasCell cell) {
        return territories.territoryAt(cell);
    }

    public String factionAt(AtlasCell cell) {
        return territories.factionAt(cell, regionOf(cell));
    }

    Map<String, Float> factionPool(AtlasCell cell) {
        return territories.factionPool(regionOf(cell));
    }

    public Map<String, String> factionDecisions(Collection<AtlasCell> revealed) {
        Set<AtlasCell> shown = new LinkedHashSet<>();
        for (AtlasCell cell : revealed) {
            shown.addAll(cell.around(1));
            shown.addAll(lanes(cell));
        }
        Map<String, String> decisions = new HashMap<>();
        for (AtlasCell cell : shown) {
            territories.recordFactions(cell, regionOf(cell), decisions::putIfAbsent);
        }
        return decisions;
    }

    private boolean computeVoid(AtlasCell cell) {
        if (specials.fixedCells().contains(cell.pack()) || cell.equals(AtlasCell.ORIGIN)) {
            return false;
        }
        if (terrain.clouded(displayX(cell), displayY(cell), AtlasTerrain.SYSTEM_CLEARANCE)) {
            return true;
        }
        if (AtlasPositions.isLattice(cell)) {
            return false;
        }
        float keep = (cell.mayBeVoid() ? 1f - rules.voidRule().chanceAt(cell.centreDistance()) : 1f) * regionOf(cell).density();
        return AtlasHash.unit(AtlasHash.of(seed, VOID_SALT, cell.gridX(), cell.gridY())) < 1f - keep;
    }

    private AtlasNode node(AtlasCell cell, Map<Long, AtlasSpecialPlacer.PlacedSpecial> placed) {
        float x = mapX(cell);
        float y = mapY(cell);
        float distance = (float) Math.hypot(x, y);
        AtlasRegion region = regionOf(cell);
        String factionId = territories.factionAt(cell, region);
        boolean origin = cell.equals(AtlasCell.ORIGIN);
        if (origin || isVoid(cell)) {
            return new AtlasNode(cell, x, y, !origin, region.id(), factionId, null, List.of(), null, null, lanes(cell));
        }
        List<AtlasCell> lanes = lanes(cell);
        AtlasTraits traits = traits(cell, distance, region, factionId, lanes, placed);
        String layoutId = traits.layoutId();
        List<String> anomalyIds = rollAnomalies(cell, traits, region);
        return new AtlasNode(cell, x, y, false, region.id(), factionId, layoutId, anomalyIds, traits.specialId(), traits.infestationId(), lanes);
    }

    public String infestationAt(AtlasCell cell) {
        AtlasSpecialPlacer.PlacedSpecial infestation = infestationOf(cell, regionOf(cell), specials.specialsAround(cell.chunkX(), cell.chunkY()));
        return infestation == null ? null : infestation.special().id();
    }

    private AtlasSpecialPlacer.PlacedSpecial infestationOf(AtlasCell cell, AtlasRegion region, Map<Long, AtlasSpecialPlacer.PlacedSpecial> placed) {
        AtlasSpecialPlacer.PlacedSpecial infestation = specials.nearestInfestation(cell, placed);
        return infestation == null || keepsOut(region, infestation.special()) ? null : infestation;
    }

    static boolean keepsOut(AtlasRegion region, AtlasSpecial infestation) {
        return region.factionWeights().keySet().equals(THREAT_ONLY) && !THREAT_ONLY.contains(infestation.factionId());
    }

    private List<AtlasCell> relativeNeighbours(AtlasCell cell) {
        float ax = mapX(cell);
        float ay = mapY(cell);
        List<AtlasCell> nearby = new ArrayList<>();
        List<float[]> nearbyPositions = new ArrayList<>();
        for (AtlasCell other : cell.around(LANE_WINDOW)) {
            if (other.equals(cell) || isVoid(other)) {
                continue;
            }
            float ox = mapX(other);
            float oy = mapY(other);
            if (AtlasPositions.squared(ox - ax, oy - ay) <= LONGEST_LANE * LONGEST_LANE) {
                nearby.add(other);
                nearbyPositions.add(new float[] {ox, oy});
            }
        }
        Sight sight = new Sight(cell, nearby);
        List<AtlasCell> lanes = new ArrayList<>();
        for (int candidate = 0; candidate < nearby.size(); candidate++) {
            if (!sight.clear(nearby.size(), candidate)) {
                continue;
            }
            float[] end = nearbyPositions.get(candidate);
            float limit = AtlasPositions.squared(end[0] - ax, end[1] - ay);
            boolean clear = true;
            for (int blocker = 0; blocker < nearby.size() && clear; blocker++) {
                if (blocker != candidate) {
                    float[] point = nearbyPositions.get(blocker);
                    clear = Math.max(AtlasPositions.squared(point[0] - ax, point[1] - ay), AtlasPositions.squared(point[0] - end[0], point[1] - end[1]))
                            >= limit || !sight.clear(nearby.size(), blocker) || !sight.clear(blocker, candidate);
                }
            }
            if (clear) {
                lanes.add(nearby.get(candidate));
            }
        }
        return List.copyOf(lanes);
    }

    private final class Sight {
        private final List<AtlasCell> cells;
        private final boolean open;
        private final Boolean[][] known;

        private Sight(AtlasCell from, List<AtlasCell> nearby) {
            cells = new ArrayList<>(nearby);
            cells.add(from);
            open = !terrain.anyCloudWithin(displayX(from), displayY(from), LONGEST_LANE + AtlasTerrain.LANE_CLEARANCE + 1f);
            known = open ? null : new Boolean[cells.size()][cells.size()];
        }

        boolean clear(int first, int second) {
            if (open) {
                return true;
            }
            Boolean cached = known[first][second];
            if (cached == null) {
                cached = laneClear(cells.get(first), cells.get(second));
                known[first][second] = cached;
                known[second][first] = cached;
            }
            return cached;
        }
    }

    private AtlasRegion regionOf(AtlasCell cell) {
        if (cell.equals(AtlasCell.ORIGIN)) {
            return rules.normalRegion();
        }
        AtlasRegion region = rules.region(biomes.biomeOf(cell).regionId());
        return region == null ? rules.normalRegion() : region;
    }

    private List<String> rollAnomalies(AtlasCell cell, AtlasTraits traits, AtlasRegion region) {
        List<AtlasAnomalyCandidate> passed = new ArrayList<>();
        Map<String, Double> rolls = new HashMap<>();
        for (AtlasAnomalyCandidate candidate : rules.anomalies()) {
            if (!unlocks.allows(candidate.requiredMod(), "")) {
                continue;
            }
            float chance = candidate.chanceRule(rules.anomalyRule()).chanceAt(traits.distance());
            if (chance <= 0f) {
                continue;
            }
            float weight = candidate.weighting().weightFor(traits, candidate.weight() * region.markerWeight(candidate.id()));
            double roll = AtlasHash.unit(AtlasHash.of(seed, ANOMALY_SALT, candidate.id(), cell.gridX(), cell.gridY()));
            if (roll < chance * weight) {
                passed.add(candidate);
                rolls.put(candidate.id(), roll);
            }
        }
        passed.sort(Comparator.<AtlasAnomalyCandidate>comparingDouble(candidate -> rolls.get(candidate.id())).thenComparing(AtlasAnomalyCandidate::id));
        List<AtlasAnomalyCandidate> kept = new ArrayList<>();
        for (AtlasAnomalyCandidate candidate : passed) {
            if (kept.stream().noneMatch(candidate::conflictsWith)) {
                kept.add(candidate);
            }
        }
        return kept.stream().map(AtlasAnomalyCandidate::id).toList();
    }

    public AtlasTraits traits(AtlasCell cell) {
        AtlasRegion region = regionOf(cell);
        return traits(cell, (float) Math.hypot(mapX(cell), mapY(cell)), region, territories.factionAt(cell, region), lanes(cell),
                specials.specialsAround(cell.chunkX(), cell.chunkY()));
    }

    private AtlasTraits traits(AtlasCell cell, float distance, AtlasRegion region, String factionId, List<AtlasCell> lanes,
                               Map<Long, AtlasSpecialPlacer.PlacedSpecial> placed) {
        String layoutId = AtlasHash.race(seed, LAYOUT_SALT, cell.gridX(), cell.gridY(),
                region.layoutWeights().isEmpty() ? rules.layoutWeights() : region.layoutWeights());
        AtlasSpecialPlacer.PlacedSpecial special = placed.get(cell.pack());
        AtlasSpecialPlacer.PlacedSpecial infestation = infestationOf(cell, region, placed);
        return new AtlasTraits(cell, distance, region.id(), factionId, territories.territoryAt(cell), layoutId, territories.laneTerritories(lanes),
                laneFactions(lanes), special == null ? null : special.special().id(), infestation == null ? null : infestation.special().id());
    }

    private List<String> laneFactions(List<AtlasCell> lanes) {
        List<String> factionIds = new ArrayList<>(lanes.size());
        for (AtlasCell neighbour : lanes) {
            String factionId = neighbour.equals(AtlasCell.ORIGIN) ? null : factionAt(neighbour);
            if (factionId != null) {
                factionIds.add(factionId);
            }
        }
        return factionIds;
    }
}
