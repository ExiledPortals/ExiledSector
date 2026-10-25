package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public final class AtlasState implements AtlasMemory {

    public static final String PERSISTENT_KEY = "exiledSector_atlasState";
    public static final int COMPLETED = 1;
    public static final int FAILED = 2;
    public static final int MARKERS_SPENT = 4;
    public static final int CLEANSED = 8;
    public static final int ATTEMPTED = 16;
    static final String FLAG_SEPARATOR = ",";
    private static final Random SEEDS = new Random();

    private final long seed;
    private final int generatorVersion;
    private Map<Long, Integer> cellFlags = new HashMap<>();
    private Map<Long, String> siteRegions = new HashMap<>();
    private Map<Long, String> chunkUnlocks = new HashMap<>();
    private Set<Integer> clearedTiers = new HashSet<>();
    private int atlasPoints;
    private int keyFragments;

    public AtlasState(long seed) {
        this.seed = seed;
        this.generatorVersion = AtlasGenerator.VERSION;
    }

    public static AtlasState get(SectorAPI sector) {
        Map<String, Object> data = sector.getPersistentData();
        if (data.get(PERSISTENT_KEY) instanceof AtlasState existing) {
            return existing.repaired();
        }
        AtlasState created = new AtlasState(SEEDS.nextLong());
        data.put(PERSISTENT_KEY, created);
        return created;
    }

    public static AtlasState getIfCreated(SectorAPI sector) {
        return sector != null && sector.getPersistentData().get(PERSISTENT_KEY) instanceof AtlasState existing ? existing.repaired() : null;
    }

    AtlasState repaired() {
        if (cellFlags == null) {
            cellFlags = new HashMap<>();
        }
        if (siteRegions == null) {
            siteRegions = new HashMap<>();
        }
        if (chunkUnlocks == null) {
            chunkUnlocks = new HashMap<>();
        }
        if (clearedTiers == null) {
            clearedTiers = new HashSet<>();
        }
        return this;
    }

    public long seed() {
        return seed;
    }

    public int generatorVersion() {
        return generatorVersion;
    }

    public boolean has(AtlasCell cell, int flag) {
        if (flag == COMPLETED && cell.equals(AtlasCell.ORIGIN)) {
            return true;
        }
        return (cellFlags.getOrDefault(cell.pack(), 0) & flag) != 0;
    }

    public void add(AtlasCell cell, int flag) {
        cellFlags.merge(cell.pack(), flag, (current, added) -> current | added);
    }

    public boolean isCompleted(AtlasCell cell) {
        return has(cell, COMPLETED);
    }

    public Set<AtlasCell> completedCells() {
        Set<AtlasCell> completed = new LinkedHashSet<>(List.of(AtlasCell.ORIGIN));
        cellFlags.forEach((packed, flags) -> {
            if ((flags & COMPLETED) != 0) {
                completed.add(AtlasCell.unpack(packed));
            }
        });
        return completed;
    }

    @Override
    public String recordedRegion(long siteKey) {
        return siteRegions.get(siteKey);
    }

    @Override
    public Boolean unlockedAtReveal(long chunkKey, String unlock) {
        String recorded = chunkUnlocks.get(chunkKey);
        if (recorded == null) {
            return null;
        }
        return List.of(recorded.split(FLAG_SEPARATOR)).contains(unlock);
    }

    public void recordReveal(AtlasGenerator generator, Collection<AtlasCell> revealed) {
        String flags = String.join(FLAG_SEPARATOR, new TreeSet<>(generator.unlockFlags()));
        for (AtlasCell cell : revealed) {
            for (AtlasBiome biome : generator.biomeContenders(cell)) {
                siteRegions.putIfAbsent(biome.siteKey(), biome.regionId());
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (chunkUnlocks.putIfAbsent(AtlasGenerator.chunkKey(cell.chunkX() + dx, cell.chunkY() + dy), flags) == null) {
                        recordChunkSites(generator, cell.chunkX() + dx, cell.chunkY() + dy);
                    }
                }
            }
        }
    }

    private void recordChunkSites(AtlasGenerator generator, int chunkX, int chunkY) {
        for (AtlasCell cell : AtlasSpecialPlacer.cellsOf(chunkX, chunkY)) {
            for (AtlasBiome biome : generator.biomeContenders(cell)) {
                siteRegions.putIfAbsent(biome.siteKey(), biome.regionId());
            }
        }
    }

    public int recordedSites() {
        return siteRegions.size();
    }

    public boolean clearTier(int tier) {
        if (!clearedTiers.add(tier)) {
            return false;
        }
        atlasPoints++;
        return true;
    }

    public Set<Integer> clearedTiers() {
        return new TreeSet<>(clearedTiers);
    }

    public void addAtlasPoints(int points) {
        atlasPoints += Math.max(0, points);
    }

    public int atlasPoints() {
        return atlasPoints;
    }

    public void addKeyFragment() {
        keyFragments++;
    }

    public int keyFragments() {
        return keyFragments;
    }
}
