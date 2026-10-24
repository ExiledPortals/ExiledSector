package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AtlasBiomes {

    static final float BIOME_SPACING = 4f;
    static final float BIOME_GAP = 1f;
    static final int SITE_WINDOW = 3;
    static final int PATCH_WINDOW = 2;
    static final int CLAIM_WINDOW = 7;
    static final int MIN_BIOME_SYSTEMS = 4;
    static final int MAX_BIOME_SYSTEMS = 7;
    private static final int CACHE_LIMIT = 4096;
    private static final long SITE_POINT_SALT = 0x53495445505443L;
    private static final long SITE_PRIORITY_SALT = 0x5349544550524fL;
    private static final long REGION_PICK_SALT = 0x5245474e50434bL;
    private static final long SIZE_SALT = 0x53495a45L;
    private static final long AXIS_SALT = 0x59L;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasUnlocks unlocks;
    private final AtlasMemory memory;
    private final AtlasPositions positions;
    private final Set<Long> fixedCells;
    private final Map<Long, Boolean> survivalCache = AtlasPositions.boundedCache(CACHE_LIMIT);
    private final Map<Long, Patch> patchCache = AtlasPositions.boundedCache(CACHE_LIMIT);
    private final Map<Long, AtlasBiome> biomeCache = AtlasPositions.boundedCache(CACHE_LIMIT);

    private record Site(int siteX, int siteY, float centreX, float centreY, double priority) {

        long key() {
            return new AtlasCell(siteX, siteY).pack();
        }
    }

    private record Patch(Site site, float radius, List<AtlasCell> systems, boolean blocked) {
    }

    AtlasBiomes(long seed, AtlasRules rules, AtlasUnlocks unlocks, AtlasMemory memory, AtlasPositions positions, Set<Long> fixedCells) {
        this.seed = seed;
        this.rules = rules;
        this.unlocks = unlocks;
        this.memory = memory;
        this.positions = positions;
        this.fixedCells = fixedCells;
    }

    AtlasBiome biomeOf(AtlasCell cell) {
        if (!AtlasPositions.isLattice(cell) || cell.equals(AtlasCell.ORIGIN)) {
            return null;
        }
        for (AtlasBiome biome : biomesNear(cell)) {
            if (biome.systems().contains(cell)) {
                return biome;
            }
        }
        return null;
    }

    List<AtlasBiome> biomesAround(int chunkX, int chunkY) {
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

    AtlasBiome coveringBiome(AtlasCell cell) {
        float x = positions.mapX(cell);
        float y = positions.mapY(cell);
        for (AtlasBiome biome : biomesNear(cell)) {
            if (biome.covers(x, y)) {
                return biome;
            }
        }
        return null;
    }

    List<AtlasBiome> sitesTouching(AtlasCell cell) {
        float x = positions.mapX(cell);
        float y = positions.mapY(cell);
        List<AtlasBiome> touching = new ArrayList<>();
        for (AtlasBiome biome : biomesNear(cell)) {
            if (biome.covers(x, y) || biome.systems().contains(cell)) {
                touching.add(biome);
            }
        }
        return touching;
    }

    private List<AtlasBiome> biomesNear(AtlasCell cell) {
        int siteX = (int) Math.floor(positions.mapX(cell) / BIOME_SPACING);
        int siteY = (int) Math.floor(positions.mapY(cell) / BIOME_SPACING);
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
            if (AtlasPositions.isLattice(cell)) {
                lattice.add(cell);
            }
        }
        lattice.sort(Comparator.comparingDouble((AtlasCell cell) -> AtlasPositions.squared(positions.mapX(cell) - site.centreX(),
                positions.mapY(cell) - site.centreY())).thenComparingLong(AtlasCell::pack));
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
        return (float) Math.hypot(positions.mapX(cell) - site.centreX(), positions.mapY(cell) - site.centreY());
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
                boolean tooClose = AtlasPositions.squared(rival.centreX() - site.centreX(), rival.centreY() - site.centreY()) < reach * reach;
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
}
