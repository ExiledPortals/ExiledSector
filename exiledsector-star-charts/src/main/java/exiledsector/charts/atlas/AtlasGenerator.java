package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasGenerator {

    public static final int VERSION = 1;
    static final float LONGEST_LANE = (float) (3.0 * Math.sqrt(2.0));
    static final int LANE_WINDOW = 5;
    private static final int CACHE_LIMIT_CELLS = 65536;
    private static final long VOID_SALT = 0x564f4944L;
    private static final long LAYOUT_SALT = 0x4c41594fL;
    private static final long BOSS_SALT = 0x424f5353L;
    private static final long EXPANSION_SALT = 0x455850414eL;
    private static final long PHENOMENON_SALT = 0x5048454eL;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasUnlocks unlocks;
    private final AtlasPositions positions;
    private final AtlasSpecialPlacer specials;
    private final AtlasBiomes biomes;
    private final AtlasTerritories territories;
    private final Map<Long, List<AtlasCell>> laneCache = AtlasPositions.boundedCache(CACHE_LIMIT_CELLS);
    private final Map<Long, Boolean> voidCache = AtlasPositions.boundedCache(CACHE_LIMIT_CELLS);

    public AtlasGenerator(long seed, AtlasRules rules, AtlasUnlocks unlocks, AtlasMemory memory) {
        this.seed = seed;
        this.rules = rules;
        this.unlocks = unlocks;
        positions = new AtlasPositions(seed);
        specials = new AtlasSpecialPlacer(seed, rules, unlocks, memory, positions, this::isVoid);
        biomes = new AtlasBiomes(seed, rules, unlocks, memory, positions, specials.fixedCells());
        territories = new AtlasTerritories(seed, rules, positions);
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

    public List<AtlasCell> lanes(AtlasCell cell) {
        if (isVoid(cell)) {
            return List.of();
        }
        return laneCache.computeIfAbsent(cell.pack(), packed -> gabrielNeighbours(cell));
    }

    public AtlasBiome biomeOf(AtlasCell cell) {
        return biomes.biomeOf(cell);
    }

    public List<AtlasBiome> biomesAround(int chunkX, int chunkY) {
        return biomes.biomesAround(chunkX, chunkY);
    }

    public List<AtlasBiome> sitesTouching(AtlasCell cell) {
        return biomes.sitesTouching(cell);
    }

    public AtlasSpecial specialAt(AtlasCell cell) {
        return specials.specialAt(cell);
    }

    public AtlasCell infestationSource(AtlasCell cell) {
        AtlasSpecialPlacer.PlacedSpecial source = specials.nearestInfestation(cell, specials.specialsAround(cell.chunkX(), cell.chunkY()));
        return source == null ? null : source.cell();
    }

    String territoryAt(AtlasCell cell) {
        return territories.territoryAt(cell);
    }

    private boolean computeVoid(AtlasCell cell) {
        if (specials.fixedCells().contains(cell.pack()) || cell.equals(AtlasCell.ORIGIN)) {
            return false;
        }
        if (cell.mayBeVoid() && AtlasHash.unit(AtlasHash.of(seed, VOID_SALT, cell.gridX(), cell.gridY())) < rules.voidRule().chanceAt(cell.centreDistance())) {
            return true;
        }
        return !AtlasPositions.isLattice(cell) && biomes.coveringBiome(cell) != null;
    }

    private AtlasNode node(AtlasCell cell, Map<Long, AtlasSpecialPlacer.PlacedSpecial> placed) {
        float x = mapX(cell);
        float y = mapY(cell);
        float distance = (float) Math.hypot(x, y);
        AtlasRegion region = regionOf(cell);
        String factionId = territories.factionAt(cell, region);
        boolean origin = cell.equals(AtlasCell.ORIGIN);
        if (origin || isVoid(cell)) {
            return new AtlasNode(cell, x, y, !origin, region.id(), factionId, null, false, null, null, null, null, lanes(cell));
        }
        List<AtlasCell> lanes = lanes(cell);
        String layoutId = AtlasHash.race(seed, LAYOUT_SALT, cell.gridX(), cell.gridY(),
                region.layoutWeights().isEmpty() ? rules.layoutWeights() : region.layoutWeights());
        boolean powerfulBoss = AtlasHash.unit(AtlasHash.of(seed, BOSS_SALT, cell.gridX(), cell.gridY())) < rules.powerfulBossRule().chanceAt(distance);
        AtlasTraits traits = new AtlasTraits(cell, distance, region.id(), factionId, territories.territoryAt(cell), layoutId,
                territories.laneTerritories(lanes));
        String phenomenonId = rollMarker(cell, traits, region, rules.phenomenonRule(), rules.phenomena(), PHENOMENON_SALT);
        String expansionId = rollMarker(cell, traits, region, rules.expansionRule(), rules.expansions(), EXPANSION_SALT);
        AtlasSpecialPlacer.PlacedSpecial special = placed.get(cell.pack());
        AtlasSpecialPlacer.PlacedSpecial infestation = specials.nearestInfestation(cell, placed);
        return new AtlasNode(cell, x, y, false, region.id(), factionId, layoutId, powerfulBoss, phenomenonId, expansionId,
                special == null ? null : special.special().id(), infestation == null ? null : infestation.special().id(), lanes);
    }

    private List<AtlasCell> gabrielNeighbours(AtlasCell cell) {
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
        List<AtlasCell> lanes = new ArrayList<>();
        for (int candidate = 0; candidate < nearby.size(); candidate++) {
            float[] end = nearbyPositions.get(candidate);
            float midX = (ax + end[0]) / 2f;
            float midY = (ay + end[1]) / 2f;
            float limit = AtlasPositions.squared(end[0] - ax, end[1] - ay) / 4f;
            boolean clear = true;
            for (int blocker = 0; blocker < nearby.size() && clear; blocker++) {
                if (blocker != candidate) {
                    float[] point = nearbyPositions.get(blocker);
                    clear = AtlasPositions.squared(point[0] - midX, point[1] - midY) >= limit;
                }
            }
            if (clear) {
                lanes.add(nearby.get(candidate));
            }
        }
        return List.copyOf(lanes);
    }

    private AtlasRegion regionOf(AtlasCell cell) {
        AtlasBiome biome = isVoid(cell) ? biomes.coveringBiome(cell) : biomes.biomeOf(cell);
        AtlasRegion region = biome == null ? null : rules.region(biome.regionId());
        return region == null ? rules.normalRegion() : region;
    }

    private String rollMarker(AtlasCell cell, AtlasTraits traits, AtlasRegion region, AtlasMarkerRule rule, List<AtlasMarkerCandidate> candidates,
                              long salt) {
        float chance = rule.chanceAt(traits.distance());
        if (chance <= 0f) {
            return null;
        }
        String winner = null;
        double winningRoll = Double.POSITIVE_INFINITY;
        for (AtlasMarkerCandidate candidate : candidates) {
            if (!unlocks.allows(candidate.requiredMod(), "")) {
                continue;
            }
            float weight = candidate.weighting().weightFor(traits, candidate.weight() * region.markerWeight(candidate.id()));
            double roll = AtlasHash.unit(AtlasHash.of(seed, salt, candidate.id(), cell.gridX(), cell.gridY()));
            if (roll < chance * weight && AtlasHash.beats(roll, candidate.id(), winningRoll, winner)) {
                winner = candidate.id();
                winningRoll = roll;
            }
        }
        return winner;
    }
}
