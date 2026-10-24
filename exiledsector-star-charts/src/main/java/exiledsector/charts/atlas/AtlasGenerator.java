package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasGenerator {

    public static final int VERSION = 1;
    static final float JITTER_REACH = 0.4f;
    static final float LONGEST_LANE = (float) (3.0 * Math.sqrt(2.0));
    static final int LANE_WINDOW = 5;
    static final float BIOME_SPACING = 4f;
    static final float BIOME_GAP = 1f;
    static final int SITE_WINDOW = 3;
    private static final int CACHE_LIMIT_CELLS = 65536;
    static final int PATCH_WINDOW = 2;
    static final int CLAIM_WINDOW = 7;
    static final int MIN_BIOME_SYSTEMS = 4;
    static final int MAX_BIOME_SYSTEMS = 7;
    static final float TERRITORY_SPACING = 16f;
    private static final int CACHE_LIMIT = 4096;
    private static final long JITTER_X_SALT = 0x4a4954585fL;
    private static final long JITTER_Y_SALT = 0x4a4954595fL;
    private static final long VOID_SALT = 0x564f4944L;
    private static final long SITE_POINT_SALT = 0x53495445505443L;
    private static final long SITE_PRIORITY_SALT = 0x5349544550524fL;
    private static final long REGION_PICK_SALT = 0x5245474e50434bL;
    private static final long SIZE_SALT = 0x53495a45L;
    private static final long TERRITORY_POINT_SALT = 0x54455252505443L;
    private static final long TERRITORY_PICK_SALT = 0x544552525043L;
    private static final long FACTION_SALT = 0x46414354L;
    private static final long LAYOUT_SALT = 0x4c41594fL;
    private static final long BOSS_SALT = 0x424f5353L;
    private static final long EXPANSION_SALT = 0x455850414eL;
    private static final long PHENOMENON_SALT = 0x5048454eL;
    private static final long SPECIAL_SALT = 0x53504543L;
    private static final long SPECIAL_CELL_SALT = 0x53504543454cL;
    private static final long AXIS_SALT = 0x59L;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasUnlocks unlocks;
    private final AtlasMemory memory;
    private final Set<Long> fixedCells = new HashSet<>();
    private final Map<Long, PlacedSpecial> fixedSpecials = new HashMap<>();
    private final Map<Long, List<AtlasCell>> laneCache = boundedCache(CACHE_LIMIT_CELLS);
    private final Map<Long, Boolean> voidCache = boundedCache(CACHE_LIMIT_CELLS);
    private final Map<Long, Boolean> survivalCache = boundedCache(CACHE_LIMIT);
    private final Map<Long, Patch> patchCache = boundedCache(CACHE_LIMIT);
    private final Map<Long, AtlasBiome> biomeCache = boundedCache(CACHE_LIMIT);
    private final Map<Long, Map<Long, PlacedSpecial>> specialCache = boundedCache(CACHE_LIMIT);

    private record PlacedSpecial(AtlasCell cell, AtlasSpecial special, double roll) {
    }

    private record Site(int siteX, int siteY, float centreX, float centreY, double priority) {

        long key() {
            return new AtlasCell(siteX, siteY).pack();
        }
    }

    private record Patch(Site site, float radius, List<AtlasCell> systems, boolean blocked) {
    }

    private record SeedPoint(int gridX, int gridY) {
    }

    public AtlasGenerator(long seed, AtlasRules rules, AtlasUnlocks unlocks) {
        this(seed, rules, unlocks, AtlasMemory.NONE);
    }

    public AtlasGenerator(long seed, AtlasRules rules, AtlasUnlocks unlocks, AtlasMemory memory) {
        this.seed = seed;
        this.rules = rules;
        this.unlocks = unlocks;
        this.memory = memory;
        for (AtlasSpecial special : rules.specials()) {
            if (special.placement() != AtlasSpecial.Placement.FIXED) {
                continue;
            }
            for (int index = 0; index < special.count(); index++) {
                double bearing = Math.PI / 2.0 + 2.0 * Math.PI * index / special.count();
                AtlasCell cell = AtlasCell.nearest((float) (Math.cos(bearing) * special.steps()), (float) (Math.sin(bearing) * special.steps()));
                fixedCells.add(cell.pack());
                if (unlocks.allows(special.requiredMod(), special.unlock())) {
                    fixedSpecials.putIfAbsent(cell.pack(), new PlacedSpecial(cell, special, -1.0));
                }
            }
        }
    }

    public long seed() {
        return seed;
    }

    public AtlasNode node(AtlasCell cell) {
        return node(cell, specialsAround(cell.chunkX(), cell.chunkY()));
    }

    public List<AtlasNode> chunk(int chunkX, int chunkY) {
        Map<Long, PlacedSpecial> specials = specialsAround(chunkX, chunkY);
        List<AtlasNode> nodes = new ArrayList<>(AtlasCell.CHUNK_SIZE * AtlasCell.CHUNK_SIZE);
        for (AtlasCell cell : cellsOf(chunkX, chunkY)) {
            nodes.add(node(cell, specials));
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

    private boolean computeVoid(AtlasCell cell) {
        if (fixedCells.contains(cell.pack()) || cell.equals(AtlasCell.ORIGIN)) {
            return false;
        }
        if (cell.mayBeVoid() && AtlasHash.unit(AtlasHash.of(seed, VOID_SALT, cell.gridX(), cell.gridY())) < rules.voidRule().chanceAt(cell.centreDistance())) {
            return true;
        }
        return !isLattice(cell) && coveringBiome(cell) != null;
    }

    public float mapX(AtlasCell cell) {
        return cell.gridX() + jitter(cell, JITTER_X_SALT);
    }

    public float mapY(AtlasCell cell) {
        return cell.gridY() + jitter(cell, JITTER_Y_SALT);
    }

    public List<AtlasCell> lanes(AtlasCell cell) {
        if (isVoid(cell)) {
            return List.of();
        }
        return laneCache.computeIfAbsent(cell.pack(), packed -> gabrielNeighbours(cell));
    }

    public AtlasBiome biomeOf(AtlasCell cell) {
        if (!isLattice(cell) || cell.equals(AtlasCell.ORIGIN)) {
            return null;
        }
        for (AtlasBiome biome : biomesNear(cell)) {
            if (biome.systems().contains(cell)) {
                return biome;
            }
        }
        return null;
    }

    public List<AtlasBiome> biomesAround(int chunkX, int chunkY) {
        int firstX = (int) Math.floor(chunkX * AtlasCell.CHUNK_SIZE / BIOME_SPACING) - PATCH_WINDOW;
        int lastX = (int) Math.floor(((chunkX + 1) * AtlasCell.CHUNK_SIZE - 1) / BIOME_SPACING) + PATCH_WINDOW;
        int firstY = (int) Math.floor(chunkY * AtlasCell.CHUNK_SIZE / BIOME_SPACING) - PATCH_WINDOW;
        int lastY = (int) Math.floor(((chunkY + 1) * AtlasCell.CHUNK_SIZE - 1) / BIOME_SPACING) + PATCH_WINDOW;
        List<AtlasBiome> biomes = new ArrayList<>();
        for (int sx = firstX; sx <= lastX; sx++) {
            for (int sy = firstY; sy <= lastY; sy++) {
                AtlasBiome biome = biomeAt(sx, sy);
                if (biome != null && !biome.isNormal()) {
                    biomes.add(biome);
                }
            }
        }
        return biomes;
    }

    private AtlasNode node(AtlasCell cell, Map<Long, PlacedSpecial> specials) {
        float x = mapX(cell);
        float y = mapY(cell);
        float distance = (float) Math.hypot(x, y);
        AtlasRegion region = regionOf(cell);
        String factionId = factionAt(cell, region);
        boolean origin = cell.equals(AtlasCell.ORIGIN);
        if (origin || isVoid(cell)) {
            return new AtlasNode(cell, x, y, !origin, region.id(), factionId, null, false, null, null, null, null, lanes(cell));
        }
        List<AtlasCell> lanes = lanes(cell);
        String layoutId = AtlasHash.race(seed, LAYOUT_SALT, cell.gridX(), cell.gridY(),
                region.layoutWeights().isEmpty() ? rules.layoutWeights() : region.layoutWeights());
        boolean powerfulBoss = AtlasHash.unit(AtlasHash.of(seed, BOSS_SALT, cell.gridX(), cell.gridY())) < rules.powerfulBossRule().chanceAt(distance);
        AtlasTraits traits = new AtlasTraits(cell, distance, region.id(), factionId, territoryAt(cell), layoutId, laneTerritories(lanes));
        String phenomenonId = rollMarker(cell, traits, region, rules.phenomenonRule(), rules.phenomena(), PHENOMENON_SALT);
        String expansionId = rollMarker(cell, traits, region, rules.expansionRule(), rules.expansions(), EXPANSION_SALT);
        PlacedSpecial special = specials.get(cell.pack());
        return new AtlasNode(cell, x, y, false, region.id(), factionId, layoutId, powerfulBoss, phenomenonId, expansionId,
                special == null ? null : special.special().id(), infestationAt(x, y, specials), lanes);
    }

    private float jitter(AtlasCell cell, long salt) {
        if (cell.equals(AtlasCell.ORIGIN)) {
            return 0f;
        }
        return (float) (AtlasHash.unit(AtlasHash.of(seed, salt, cell.gridX(), cell.gridY())) - 0.5) * 2f * JITTER_REACH;
    }

    private List<AtlasCell> gabrielNeighbours(AtlasCell cell) {
        float ax = mapX(cell);
        float ay = mapY(cell);
        List<AtlasCell> nearby = new ArrayList<>();
        List<float[]> positions = new ArrayList<>();
        for (AtlasCell other : cell.around(LANE_WINDOW)) {
            if (other.equals(cell) || isVoid(other)) {
                continue;
            }
            float ox = mapX(other);
            float oy = mapY(other);
            if (squared(ox - ax, oy - ay) <= LONGEST_LANE * LONGEST_LANE) {
                nearby.add(other);
                positions.add(new float[] {ox, oy});
            }
        }
        List<AtlasCell> lanes = new ArrayList<>();
        for (int candidate = 0; candidate < nearby.size(); candidate++) {
            float[] end = positions.get(candidate);
            float midX = (ax + end[0]) / 2f;
            float midY = (ay + end[1]) / 2f;
            float limit = squared(end[0] - ax, end[1] - ay) / 4f;
            boolean clear = true;
            for (int blocker = 0; blocker < nearby.size() && clear; blocker++) {
                if (blocker != candidate) {
                    float[] point = positions.get(blocker);
                    clear = squared(point[0] - midX, point[1] - midY) >= limit;
                }
            }
            if (clear) {
                lanes.add(nearby.get(candidate));
            }
        }
        return List.copyOf(lanes);
    }

    private AtlasRegion regionOf(AtlasCell cell) {
        AtlasBiome biome = isVoid(cell) ? coveringBiome(cell) : biomeOf(cell);
        AtlasRegion region = biome == null ? null : rules.region(biome.regionId());
        return region == null ? rules.normalRegion() : region;
    }

    private static boolean isLattice(AtlasCell cell) {
        return ((cell.gridX() | cell.gridY()) & 1) == 0;
    }

    private List<AtlasBiome> biomesNear(AtlasCell cell) {
        int siteX = (int) Math.floor(mapX(cell) / BIOME_SPACING);
        int siteY = (int) Math.floor(mapY(cell) / BIOME_SPACING);
        List<AtlasBiome> biomes = new ArrayList<>();
        for (int sx = siteX - PATCH_WINDOW; sx <= siteX + PATCH_WINDOW; sx++) {
            for (int sy = siteY - PATCH_WINDOW; sy <= siteY + PATCH_WINDOW; sy++) {
                AtlasBiome biome = biomeAt(sx, sy);
                if (biome != null) {
                    biomes.add(biome);
                }
            }
        }
        return biomes;
    }

    private AtlasBiome coveringBiome(AtlasCell cell) {
        float x = mapX(cell);
        float y = mapY(cell);
        for (AtlasBiome biome : biomesNear(cell)) {
            if (biome.covers(x, y)) {
                return biome;
            }
        }
        return null;
    }

    private Site site(int siteX, int siteY) {
        float centreX = (siteX + (float) AtlasHash.unit(AtlasHash.of(seed, SITE_POINT_SALT, siteX, siteY))) * BIOME_SPACING;
        float centreY = (siteY + (float) AtlasHash.unit(AtlasHash.of(seed, SITE_POINT_SALT ^ AXIS_SALT, siteX, siteY))) * BIOME_SPACING;
        return new Site(siteX, siteY, centreX, centreY, AtlasHash.unit(AtlasHash.of(seed, SITE_PRIORITY_SALT, siteX, siteY)));
    }

    private Patch patch(int siteX, int siteY) {
        long key = new AtlasCell(siteX, siteY).pack();
        Patch cached = patchCache.get(key);
        if (cached != null) {
            return cached;
        }
        Site site = site(siteX, siteY);
        List<AtlasCell> lattice = new ArrayList<>();
        for (AtlasCell cell : AtlasCell.nearest(site.centreX(), site.centreY()).around(CLAIM_WINDOW)) {
            if (isLattice(cell)) {
                lattice.add(cell);
            }
        }
        lattice.sort(Comparator.comparingDouble((AtlasCell cell) -> squared(mapX(cell) - site.centreX(), mapY(cell) - site.centreY()))
                .thenComparingLong(AtlasCell::pack));
        int span = MAX_BIOME_SYSTEMS - MIN_BIOME_SYSTEMS + 1;
        int size = MIN_BIOME_SYSTEMS + (int) (AtlasHash.unit(AtlasHash.of(seed, SIZE_SALT, siteX, siteY)) * span);
        float radius = (centreDistance(site, lattice.get(size - 1)) + centreDistance(site, lattice.get(size))) / 2f;
        boolean blocked = false;
        for (long fixed : fixedCells) {
            blocked |= centreDistance(site, AtlasCell.unpack(fixed)) <= radius;
        }
        blocked |= centreDistance(site, AtlasCell.ORIGIN) <= radius;
        Patch patch = new Patch(site, radius, List.copyOf(lattice.subList(0, size)), blocked);
        patchCache.put(key, patch);
        return patch;
    }

    private float centreDistance(Site site, AtlasCell cell) {
        return (float) Math.hypot(mapX(cell) - site.centreX(), mapY(cell) - site.centreY());
    }

    private boolean survives(Patch patch) {
        long key = patch.site().key();
        Boolean cached = survivalCache.get(key);
        if (cached != null) {
            return cached;
        }
        boolean survives = !patch.blocked() && !outranked(patch);
        survivalCache.put(key, survives);
        return survives;
    }

    private boolean outranked(Patch patch) {
        Site site = patch.site();
        for (int sx = site.siteX() - SITE_WINDOW; sx <= site.siteX() + SITE_WINDOW; sx++) {
            for (int sy = site.siteY() - SITE_WINDOW; sy <= site.siteY() + SITE_WINDOW; sy++) {
                if (sx == site.siteX() && sy == site.siteY()) {
                    continue;
                }
                Patch other = patch(sx, sy);
                Site rival = other.site();
                float reach = patch.radius() + other.radius() + BIOME_GAP;
                boolean tooClose = squared(rival.centreX() - site.centreX(), rival.centreY() - site.centreY()) < reach * reach;
                if (tooClose && (rival.priority() < site.priority() || rival.priority() == site.priority() && rival.key() < site.key())
                        && survives(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    private AtlasBiome biomeAt(int siteX, int siteY) {
        long key = new AtlasCell(siteX, siteY).pack();
        if (biomeCache.containsKey(key)) {
            return biomeCache.get(key);
        }
        Patch patch = patch(siteX, siteY);
        AtlasBiome biome = null;
        if (survives(patch)) {
            Site site = patch.site();
            biome = new AtlasBiome(site.key(), site.centreX(), site.centreY(), patch.radius(), siteRegion(site).id(), patch.systems());
        }
        biomeCache.put(key, biome);
        return biome;
    }

    private AtlasRegion siteRegion(Site site) {
        AtlasRegion recorded = rules.region(memory.recordedRegion(site.key()));
        if (recorded != null && unlocks.allows(recorded.requiredMod(), "")) {
            return recorded;
        }
        float distance = (float) Math.hypot(site.centreX(), site.centreY());
        Map<String, Float> weights = new LinkedHashMap<>();
        for (AtlasRegion region : rules.regions()) {
            if (region.minSteps() <= distance) {
                weights.put(region.id(), region.weight());
            }
        }
        AtlasRegion picked = rules.region(AtlasHash.race(seed, REGION_PICK_SALT, site.siteX(), site.siteY(), weights));
        return picked == null || !picked.availableWith(unlocks) ? rules.normalRegion() : picked;
    }

    String territoryAt(AtlasCell cell) {
        return territoryFor(territoryPoint(cell));
    }

    private SeedPoint territoryPoint(AtlasCell cell) {
        float px = mapX(cell) / TERRITORY_SPACING;
        float py = mapY(cell) / TERRITORY_SPACING;
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
                float distance = squared(sx - px, sy - py);
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

    private String factionAt(AtlasCell cell, AtlasRegion region) {
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

    private Set<String> laneTerritories(List<AtlasCell> lanes) {
        Set<String> territories = new HashSet<>();
        for (AtlasCell neighbour : lanes) {
            String territory = territoryAt(neighbour);
            if (territory != null) {
                territories.add(territory);
            }
        }
        return territories;
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

    private Map<Long, PlacedSpecial> specialsAround(int chunkX, int chunkY) {
        return specialCache.computeIfAbsent(chunkKey(chunkX, chunkY), key -> placeSpecialsAround(chunkX, chunkY));
    }

    private Map<Long, PlacedSpecial> placeSpecialsAround(int chunkX, int chunkY) {
        Map<Long, PlacedSpecial> placed = new HashMap<>(fixedSpecials);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                placeChunkSpecials(chunkX + dx, chunkY + dy, placed);
            }
        }
        return Map.copyOf(placed);
    }

    private void placeChunkSpecials(int chunkX, int chunkY, Map<Long, PlacedSpecial> placed) {
        List<AtlasCell> cells = null;
        for (AtlasSpecial special : rules.specials()) {
            if (special.placement() != AtlasSpecial.Placement.CHUNK || !chunkAllows(chunkX, chunkY, special)) {
                continue;
            }
            double roll = AtlasHash.unit(AtlasHash.of(seed, SPECIAL_SALT, special.id(), chunkX, chunkY));
            if (roll >= special.chance()) {
                continue;
            }
            if (cells == null) {
                cells = cellsOf(chunkX, chunkY);
            }
            AtlasCell cell = specialCell(special, cells);
            if (cell == null) {
                continue;
            }
            PlacedSpecial existing = placed.get(cell.pack());
            if (existing == null || AtlasHash.beats(roll, special.id(), existing.roll(), existing.special().id())) {
                placed.put(cell.pack(), new PlacedSpecial(cell, special, roll));
            }
        }
    }

    private boolean chunkAllows(int chunkX, int chunkY, AtlasSpecial special) {
        if (!unlocks.allows(special.requiredMod(), "")) {
            return false;
        }
        if (special.unlock().isEmpty()) {
            return true;
        }
        Boolean atReveal = memory.unlockedAtReveal(chunkKey(chunkX, chunkY), special.unlock());
        return atReveal != null ? atReveal : unlocks.allows("", special.unlock());
    }

    public static long chunkKey(int chunkX, int chunkY) {
        return new AtlasCell(chunkX, chunkY).pack();
    }

    public List<AtlasBiome> sitesTouching(AtlasCell cell) {
        float x = mapX(cell);
        float y = mapY(cell);
        List<AtlasBiome> touching = new ArrayList<>();
        for (AtlasBiome biome : biomesNear(cell)) {
            if (biome.covers(x, y) || biome.systems().contains(cell)) {
                touching.add(biome);
            }
        }
        return touching;
    }

    public AtlasSpecial specialAt(AtlasCell cell) {
        PlacedSpecial placed = specialsAround(cell.chunkX(), cell.chunkY()).get(cell.pack());
        return placed == null ? null : placed.special();
    }

    public AtlasRules rules() {
        return rules;
    }

    public Set<String> unlockFlags() {
        return unlocks.unlockedFlags();
    }

    private AtlasCell specialCell(AtlasSpecial special, List<AtlasCell> cells) {
        AtlasCell picked = null;
        double pickedRoll = Double.POSITIVE_INFINITY;
        for (AtlasCell cell : cells) {
            if (fixedCells.contains(cell.pack()) || isVoid(cell) || Math.hypot(mapX(cell), mapY(cell)) < special.minSteps()) {
                continue;
            }
            double roll = AtlasHash.unit(AtlasHash.of(seed, SPECIAL_CELL_SALT, special.id(), cell.gridX(), cell.gridY()));
            if (roll < pickedRoll) {
                pickedRoll = roll;
                picked = cell;
            }
        }
        return picked;
    }

    private String infestationAt(float x, float y, Map<Long, PlacedSpecial> specials) {
        PlacedSpecial nearest = null;
        float nearestDistance = Float.POSITIVE_INFINITY;
        for (PlacedSpecial placed : specials.values()) {
            if (!placed.special().infests()) {
                continue;
            }
            float distance = (float) Math.hypot(mapX(placed.cell()) - x, mapY(placed.cell()) - y);
            if (distance > placed.special().radius()) {
                continue;
            }
            if (distance < nearestDistance || distance == nearestDistance
                    && AtlasHash.beats(placed.roll(), placed.special().id(), nearest.roll(), nearest.special().id())) {
                nearest = placed;
                nearestDistance = distance;
            }
        }
        return nearest == null ? null : nearest.special().id();
    }

    private static List<AtlasCell> cellsOf(int chunkX, int chunkY) {
        List<AtlasCell> cells = new ArrayList<>(AtlasCell.CHUNK_SIZE * AtlasCell.CHUNK_SIZE);
        for (int x = chunkX * AtlasCell.CHUNK_SIZE; x < (chunkX + 1) * AtlasCell.CHUNK_SIZE; x++) {
            for (int y = chunkY * AtlasCell.CHUNK_SIZE; y < (chunkY + 1) * AtlasCell.CHUNK_SIZE; y++) {
                cells.add(new AtlasCell(x, y));
            }
        }
        return cells;
    }

    private static float squared(float dx, float dy) {
        return dx * dx + dy * dy;
    }

    private static <V> Map<Long, V> boundedCache(int limit) {
        return new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > limit;
            }
        };
    }
}
