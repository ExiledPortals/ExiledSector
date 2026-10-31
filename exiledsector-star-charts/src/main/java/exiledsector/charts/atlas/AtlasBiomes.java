package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AtlasBiomes {

    static final float BIOME_SPACING = 5f;
    static final float SIZE_REACH = 0.5f;
    static final float MAX_BONUS = bonus(AtlasRegion.MAX_SIZE);
    static final float MIN_BONUS = bonus(AtlasRegion.MIN_SIZE);
    static final int SITE_WINDOW = 3;
    static final float POCKET_REACH = 2f * BIOME_SPACING;
    static final int POCKET_WINDOW = 2;
    static final float SITE_REACH = BIOME_SPACING * (float) Math.sqrt(2) + MAX_BONUS - MIN_BONUS;
    static final float TOUCH_DISTANCE = 2f * SITE_REACH + (1f + 2f * AtlasPositions.JITTER_REACH) * (float) Math.sqrt(2);
    static final int TOUCH_WINDOW = (int) Math.ceil(TOUCH_DISTANCE / BIOME_SPACING) + 1;
    private static final int OWNER_CACHE_LIMIT = 4096;
    private static final int REGION_CACHE_LIMIT = 65536;
    private static final long SITE_POINT_SALT = 0x53495445505443L;
    private static final long SITE_PRIORITY_SALT = 0x5349544550524fL;
    private static final long REGION_PICK_SALT = 0x5245474e50434bL;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasUnlocks unlocks;
    private final AtlasMemory memory;
    private final AtlasPositions positions;
    private final boolean neighbourRules;
    private final boolean boostingRules;
    private final Set<String> ruledRegions = new HashSet<>();
    private final JitteredGrid sitePoints;
    private final Map<Long, AtlasRegion> regionCache = AtlasPositions.boundedCache(REGION_CACHE_LIMIT);
    private final Map<Long, AtlasBiome> ownerCache = AtlasPositions.boundedCache(OWNER_CACHE_LIMIT);

    record Border(AtlasBiome owner, AtlasBiome runnerUp, float gap) {
    }

    private record Site(int siteX, int siteY, float centreX, float centreY, double priority) {

        long key() {
            return new AtlasCell(siteX, siteY).pack();
        }

        boolean outranks(Site other) {
            return priority < other.priority || priority == other.priority && key() < other.key();
        }

        float distanceTo(float x, float y) {
            return (float) Math.hypot(x - centreX, y - centreY);
        }
    }

    AtlasBiomes(long seed, AtlasRules rules, AtlasUnlocks unlocks, AtlasMemory memory, AtlasPositions positions) {
        this.seed = seed;
        this.sitePoints = new JitteredGrid(seed, SITE_POINT_SALT);
        this.rules = rules;
        this.unlocks = unlocks;
        this.memory = memory;
        this.positions = positions;
        for (AtlasRegion region : rules.regions()) {
            if (!region.neighbourWeights().isEmpty()) {
                ruledRegions.add(region.id());
                ruledRegions.addAll(region.neighbourWeights().keySet());
            }
        }
        neighbourRules = !ruledRegions.isEmpty();
        boostingRules = rules.regions().stream().anyMatch(region -> region.neighbourWeights().values().stream().anyMatch(weight -> weight > 1f));
    }

    AtlasBiome biomeOf(AtlasCell cell) {
        return ownerCache.computeIfAbsent(cell.pack(), packed -> owner(positions.mapX(cell), positions.mapY(cell)));
    }

    List<AtlasBiome> contenders(AtlasCell cell, float extraReach) {
        float x = positions.mapX(cell);
        float y = positions.mapY(cell);
        List<AtlasBiome> contenders = new ArrayList<>();
        for (Site site : contenderSites(x, y, extraReach)) {
            contenders.add(biome(site));
        }
        return contenders;
    }

    List<AtlasBiome> biomesAround(int chunkX, int chunkY) {
        int firstX = (int) Math.floor(chunkX * AtlasCell.CHUNK_SIZE / BIOME_SPACING) - 1;
        int lastX = (int) Math.floor(((chunkX + 1) * AtlasCell.CHUNK_SIZE - 1) / BIOME_SPACING) + 1;
        int firstY = (int) Math.floor(chunkY * AtlasCell.CHUNK_SIZE / BIOME_SPACING) - 1;
        int lastY = (int) Math.floor(((chunkY + 1) * AtlasCell.CHUNK_SIZE - 1) / BIOME_SPACING) + 1;
        List<AtlasBiome> biomes = new ArrayList<>();
        for (int sx = firstX; sx <= lastX; sx++) {
            for (int sy = firstY; sy <= lastY; sy++) {
                biomes.add(biome(site(sx, sy)));
            }
        }
        return biomes;
    }

    static float bonus(float size) {
        return (size - 1f) * SIZE_REACH * BIOME_SPACING;
    }

    Border borderAt(float x, float y) {
        int baseX = (int) Math.floor(x / BIOME_SPACING);
        int baseY = (int) Math.floor(y / BIOME_SPACING);
        Site best = null;
        Site second = null;
        float bestScore = Float.POSITIVE_INFINITY;
        float secondScore = Float.POSITIVE_INFINITY;
        for (int sx = baseX - SITE_WINDOW; sx <= baseX + SITE_WINDOW; sx++) {
            for (int sy = baseY - SITE_WINDOW; sy <= baseY + SITE_WINDOW; sy++) {
                Site site = site(sx, sy);
                float score = site.distanceTo(x, y) - siteBonus(site);
                if (beats(score, site, bestScore, best)) {
                    second = best;
                    secondScore = bestScore;
                    best = site;
                    bestScore = score;
                } else if (beats(score, site, secondScore, second)) {
                    second = site;
                    secondScore = score;
                }
            }
        }
        return new Border(biome(best), biome(second), secondScore - bestScore);
    }

    private static boolean beats(float score, Site site, float bestScore, Site best) {
        return score < bestScore || score == bestScore && best != null && site.key() < best.key();
    }

    private AtlasBiome owner(float x, float y) {
        Site best = null;
        float bestScore = Float.POSITIVE_INFINITY;
        for (Site site : contenderSites(x, y, 0f)) {
            float score = site.distanceTo(x, y) - siteBonus(site);
            if (beats(score, site, bestScore, best)) {
                bestScore = score;
                best = site;
            }
        }
        return biome(best);
    }

    private List<Site> contenderSites(float x, float y, float extraReach) {
        int baseX = (int) Math.floor(x / BIOME_SPACING);
        int baseY = (int) Math.floor(y / BIOME_SPACING);
        List<Site> window = new ArrayList<>();
        float nearest = Float.POSITIVE_INFINITY;
        for (int sx = baseX - SITE_WINDOW; sx <= baseX + SITE_WINDOW; sx++) {
            for (int sy = baseY - SITE_WINDOW; sy <= baseY + SITE_WINDOW; sy++) {
                Site site = site(sx, sy);
                window.add(site);
                nearest = Math.min(nearest, site.distanceTo(x, y));
            }
        }
        float reach = nearest + MAX_BONUS - MIN_BONUS + extraReach;
        window.removeIf(site -> site.distanceTo(x, y) > reach);
        return window;
    }

    private AtlasBiome biome(Site site) {
        return new AtlasBiome(site.key(), site.centreX(), site.centreY(), siteRegion(site).id());
    }

    private float siteBonus(Site site) {
        return bonus(siteRegion(site).size());
    }

    private Site site(int siteX, int siteY) {
        float centreX = sitePoints.x(siteX, siteY) * BIOME_SPACING;
        float centreY = sitePoints.y(siteX, siteY) * BIOME_SPACING;
        return new Site(siteX, siteY, centreX, centreY, AtlasHash.unit(AtlasHash.of(seed, SITE_PRIORITY_SALT, siteX, siteY)));
    }

    private AtlasRegion siteRegion(Site site) {
        long key = site.key();
        AtlasRegion cached = regionCache.get(key);
        if (cached != null) {
            return cached;
        }
        AtlasRegion region = pickRegion(site);
        regionCache.put(key, region);
        return region;
    }

    private AtlasRegion pickRegion(Site site) {
        AtlasRegion recorded = recordedRegion(site);
        if (recorded != null) {
            return recorded;
        }
        float distance = (float) Math.hypot(site.centreX(), site.centreY());
        Map<String, Float> weights = new LinkedHashMap<>();
        for (AtlasRegion region : rules.regions()) {
            if (region.availableWith(unlocks)) {
                weights.put(region.id(), region.weightAt(distance));
            }
        }
        String picked = AtlasHash.race(seed, REGION_PICK_SALT, site.siteX(), site.siteY(), weights);
        if (neighbourRules && (boostingRules || ruledRegions.contains(picked))) {
            List<AtlasRegion> neighbours = decidedNeighbours(site);
            weights.replaceAll((id, weight) -> {
                AtlasRegion region = rules.region(id);
                float ruled = weight;
                for (AtlasRegion neighbour : neighbours) {
                    ruled *= region.neighbourWeight(neighbour.id()) * neighbour.neighbourWeight(id);
                }
                return ruled;
            });
            picked = AtlasHash.race(seed, REGION_PICK_SALT, site.siteX(), site.siteY(), weights);
        }
        while (picked != null && !AtlasRegion.NORMAL.equals(picked) && kinNearby(site, picked)) {
            weights.put(picked, 0f);
            picked = AtlasHash.race(seed, REGION_PICK_SALT, site.siteX(), site.siteY(), weights);
        }
        AtlasRegion region = rules.region(picked);
        return region == null ? rules.normalRegion() : region;
    }

    private boolean kinNearby(Site site, String regionId) {
        for (int sx = site.siteX() - POCKET_WINDOW; sx <= site.siteX() + POCKET_WINDOW; sx++) {
            for (int sy = site.siteY() - POCKET_WINDOW; sy <= site.siteY() + POCKET_WINDOW; sy++) {
                Site other = site(sx, sy);
                if (sx == site.siteX() && sy == site.siteY() || other.distanceTo(site.centreX(), site.centreY()) > POCKET_REACH) {
                    continue;
                }
                AtlasRegion decided = recordedRegion(other);
                if (decided == null && other.outranks(site)) {
                    decided = siteRegion(other);
                }
                if (decided != null && decided.id().equals(regionId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private AtlasRegion recordedRegion(Site site) {
        AtlasRegion recorded = rules.region(memory.recordedRegion(site.key()));
        return recorded != null && unlocks.allows(recorded.requiredMod(), "") ? recorded : null;
    }

    private List<AtlasRegion> decidedNeighbours(Site site) {
        List<AtlasRegion> neighbours = new ArrayList<>();
        for (int sx = site.siteX() - TOUCH_WINDOW; sx <= site.siteX() + TOUCH_WINDOW; sx++) {
            for (int sy = site.siteY() - TOUCH_WINDOW; sy <= site.siteY() + TOUCH_WINDOW; sy++) {
                if (sx == site.siteX() && sy == site.siteY()) {
                    continue;
                }
                Site other = site(sx, sy);
                if (other.distanceTo(site.centreX(), site.centreY()) > TOUCH_DISTANCE) {
                    continue;
                }
                AtlasRegion recorded = recordedRegion(other);
                if (recorded != null) {
                    neighbours.add(recorded);
                } else if (other.outranks(site)) {
                    neighbours.add(siteRegion(other));
                }
            }
        }
        return neighbours;
    }
}
