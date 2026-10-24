package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class AtlasSpecialPlacer {

    private static final int CACHE_LIMIT = 4096;
    private static final long SPECIAL_SALT = 0x53504543L;
    private static final long SPECIAL_CELL_SALT = 0x53504543454cL;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasUnlocks unlocks;
    private final AtlasMemory memory;
    private final AtlasPositions positions;
    private final Predicate<AtlasCell> isVoid;
    private final Set<Long> fixedCells = new HashSet<>();
    private final Map<Long, PlacedSpecial> fixedSpecials = new HashMap<>();
    private final Map<Long, Map<Long, PlacedSpecial>> specialCache = AtlasPositions.boundedCache(CACHE_LIMIT);

    record PlacedSpecial(AtlasCell cell, AtlasSpecial special, double roll) {
    }

    AtlasSpecialPlacer(long seed, AtlasRules rules, AtlasUnlocks unlocks, AtlasMemory memory, AtlasPositions positions, Predicate<AtlasCell> isVoid) {
        this.seed = seed;
        this.rules = rules;
        this.unlocks = unlocks;
        this.memory = memory;
        this.positions = positions;
        this.isVoid = isVoid;
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

    Set<Long> fixedCells() {
        return fixedCells;
    }

    Map<Long, PlacedSpecial> specialsAround(int chunkX, int chunkY) {
        return specialCache.computeIfAbsent(AtlasGenerator.chunkKey(chunkX, chunkY), key -> placeSpecialsAround(chunkX, chunkY));
    }

    AtlasSpecial specialAt(AtlasCell cell) {
        PlacedSpecial placed = specialsAround(cell.chunkX(), cell.chunkY()).get(cell.pack());
        return placed == null ? null : placed.special();
    }

    PlacedSpecial nearestInfestation(AtlasCell cell, Map<Long, PlacedSpecial> specials) {
        float x = positions.mapX(cell);
        float y = positions.mapY(cell);
        PlacedSpecial nearest = null;
        float nearestDistance = Float.POSITIVE_INFINITY;
        for (PlacedSpecial placed : specials.values()) {
            if (!placed.special().infests()) {
                continue;
            }
            float distance = (float) Math.hypot(positions.mapX(placed.cell()) - x, positions.mapY(placed.cell()) - y);
            if (distance > placed.special().radius()) {
                continue;
            }
            if (distance < nearestDistance || distance == nearestDistance
                    && AtlasHash.beats(placed.roll(), placed.special().id(), nearest.roll(), nearest.special().id())) {
                nearest = placed;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    static List<AtlasCell> cellsOf(int chunkX, int chunkY) {
        List<AtlasCell> cells = new ArrayList<>(AtlasCell.CHUNK_SIZE * AtlasCell.CHUNK_SIZE);
        for (int x = chunkX * AtlasCell.CHUNK_SIZE; x < (chunkX + 1) * AtlasCell.CHUNK_SIZE; x++) {
            for (int y = chunkY * AtlasCell.CHUNK_SIZE; y < (chunkY + 1) * AtlasCell.CHUNK_SIZE; y++) {
                cells.add(new AtlasCell(x, y));
            }
        }
        return cells;
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
        Boolean atReveal = memory.unlockedAtReveal(AtlasGenerator.chunkKey(chunkX, chunkY), special.unlock());
        return atReveal != null ? atReveal : unlocks.allows("", special.unlock());
    }

    private AtlasCell specialCell(AtlasSpecial special, List<AtlasCell> cells) {
        AtlasCell picked = null;
        double pickedRoll = Double.POSITIVE_INFINITY;
        for (AtlasCell cell : cells) {
            if (fixedCells.contains(cell.pack()) || isVoid.test(cell) || Math.hypot(positions.mapX(cell), positions.mapY(cell)) < special.minSteps()) {
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
}
