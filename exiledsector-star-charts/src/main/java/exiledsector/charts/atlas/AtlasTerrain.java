package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AtlasTerrain {

    static final int TILES_PER_CELL = 4;
    static final float SYSTEM_CLEARANCE = 0.65f;
    static final float LANE_CLEARANCE = 0.4f;
    static final float ORIGIN_CLEARANCE = 2.5f;
    static final float RIDGE_WIDTH = 2.2f;
    static final float PASS_SHARE = 0.35f;
    static final float RIDGE_NOISE_CELLS = 3f;
    static final float CLUMP_NOISE_CELLS = 3f;
    static final float CLUMP_REACH = 0.7f;
    static final float RECORD_REACH = RIDGE_WIDTH + 2.6f;
    static final int HUB_SPACING = 10;
    static final int HUB_JITTER_STEPS = 2;
    static final float CORRIDOR_MARGIN = 0.01f;
    private static final int CACHE_LIMIT = 65536;
    private static final int HUB_CACHE_LIMIT = 4096;
    private static final long WALL_SALT = 0x57414c4cL;
    private static final long RIDGE_SALT = 0x52494447L;
    private static final long CLUMP_SALT = 0x434c554dL;
    private static final long OCTAVE_SALT = 0x4f4354L;
    private static final long HUB_X_SALT = 0x4855425fL;
    private static final long HUB_Y_SALT = 0x48554259L;

    private final long seed;
    private final AtlasRules rules;
    private final AtlasBiomes biomes;
    private final AtlasPositions positions;
    private final List<float[]> fixedPoints = new ArrayList<>();
    private final List<Corridor> spurs = new ArrayList<>();
    private final boolean anyClouds;
    private final Map<Long, Integer> blocks = AtlasPositions.boundedCache(CACHE_LIMIT);
    private final Map<Long, List<Corridor>> hubCorridors = AtlasPositions.boundedCache(HUB_CACHE_LIMIT);

    private record Corridor(float[] points, float minX, float minY, float maxX, float maxY) {

        boolean clears(float x, float y) {
            float reach = SYSTEM_CLEARANCE + CORRIDOR_MARGIN;
            if (x < minX - reach || x > maxX + reach || y < minY - reach || y > maxY + reach) {
                return false;
            }
            float lineReach = LANE_CLEARANCE + CORRIDOR_MARGIN;
            for (int index = 0; index < points.length; index += 2) {
                if (AtlasPositions.squared(points[index] - x, points[index + 1] - y) <= reach * reach) {
                    return true;
                }
                if (index + 3 < points.length
                        && segmentSquared(points[index], points[index + 1], points[index + 2], points[index + 3], x, y) <= lineReach * lineReach) {
                    return true;
                }
            }
            return false;
        }
    }

    AtlasTerrain(long seed, AtlasRules rules, AtlasBiomes biomes, AtlasPositions positions, Set<Long> fixedCells) {
        this.seed = seed;
        this.rules = rules;
        this.biomes = biomes;
        this.positions = positions;
        for (long packed : fixedCells) {
            AtlasCell cell = AtlasCell.unpack(packed);
            fixedPoints.add(new float[] {positions.displayX(cell), positions.displayY(cell)});
            AtlasCell hub = hub(seed, Math.round(cell.gridX() / (float) HUB_SPACING), Math.round(cell.gridY() / (float) HUB_SPACING));
            List<AtlasCell> path = new ArrayList<>(List.of(cell));
            for (AtlasCell step : latticePath(new AtlasCell(2 * Math.round(cell.gridX() / 2f), 2 * Math.round(cell.gridY() / 2f)), hub)) {
                if (!step.equals(cell)) {
                    path.add(step);
                }
            }
            spurs.add(corridor(path));
        }
        anyClouds = rules.regions().stream().anyMatch(region -> region.clouds() > 0f || region.walls() > 0f);
    }

    boolean cloudAt(float x, float y) {
        return cloudTile((int) Math.floor(x * TILES_PER_CELL), (int) Math.floor(y * TILES_PER_CELL));
    }

    boolean cloudTile(int tileX, int tileY) {
        if (!anyClouds) {
            return false;
        }
        int mask = mask(Math.floorDiv(tileX, TILES_PER_CELL), Math.floorDiv(tileY, TILES_PER_CELL));
        return (mask >> bit(Math.floorMod(tileX, TILES_PER_CELL), Math.floorMod(tileY, TILES_PER_CELL)) & 1) != 0;
    }

    boolean clouded(float x, float y, float clearance) {
        if (!anyClouds) {
            return false;
        }
        int firstX = (int) Math.floor((x - clearance) * TILES_PER_CELL);
        int lastX = (int) Math.floor((x + clearance) * TILES_PER_CELL);
        int firstY = (int) Math.floor((y - clearance) * TILES_PER_CELL);
        int lastY = (int) Math.floor((y + clearance) * TILES_PER_CELL);
        for (int tileX = firstX; tileX <= lastX; tileX++) {
            for (int tileY = firstY; tileY <= lastY; tileY++) {
                if (AtlasPositions.squared(centre(tileX) - x, centre(tileY) - y) <= clearance * clearance && cloudTile(tileX, tileY)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean anyCloudWithin(float x, float y, float reach) {
        if (!anyClouds) {
            return false;
        }
        for (int blockX = (int) Math.floor(x - reach); blockX <= (int) Math.floor(x + reach); blockX++) {
            for (int blockY = (int) Math.floor(y - reach); blockY <= (int) Math.floor(y + reach); blockY++) {
                if (mask(blockX, blockY) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean laneClear(float fromX, float fromY, float toX, float toY) {
        if (!anyClouds) {
            return true;
        }
        boolean swap = fromX > toX || fromX == toX && fromY > toY;
        float ax = swap ? toX : fromX;
        float ay = swap ? toY : fromY;
        float bx = swap ? fromX : toX;
        float by = swap ? fromY : toY;
        float dx = bx - ax;
        float dy = by - ay;
        float lengthSquared = dx * dx + dy * dy;
        float clearance = LANE_CLEARANCE;
        int firstX = (int) Math.floor((Math.min(ax, bx) - clearance) * TILES_PER_CELL);
        int lastX = (int) Math.floor((Math.max(ax, bx) + clearance) * TILES_PER_CELL);
        int firstY = (int) Math.floor((Math.min(ay, by) - clearance) * TILES_PER_CELL);
        int lastY = (int) Math.floor((Math.max(ay, by) + clearance) * TILES_PER_CELL);
        for (int tileX = firstX; tileX <= lastX; tileX++) {
            for (int tileY = firstY; tileY <= lastY; tileY++) {
                float px = centre(tileX);
                float py = centre(tileY);
                float along = lengthSquared <= 0f ? 0f : Math.max(0f, Math.min(1f, ((px - ax) * dx + (py - ay) * dy) / lengthSquared));
                if (AtlasPositions.squared(ax + dx * along - px, ay + dy * along - py) <= clearance * clearance && cloudTile(tileX, tileY)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int mask(int blockX, int blockY) {
        return blocks.computeIfAbsent(new AtlasCell(blockX, blockY).pack(), key -> blockMask(blockX, blockY));
    }

    private int blockMask(int blockX, int blockY) {
        int mask = 0;
        for (int localX = 0; localX < TILES_PER_CELL; localX++) {
            for (int localY = 0; localY < TILES_PER_CELL; localY++) {
                int tileX = blockX * TILES_PER_CELL + localX;
                int tileY = blockY * TILES_PER_CELL + localY;
                if (raw(centre(tileX), centre(tileY))) {
                    mask |= 1 << bit(localX, localY);
                }
            }
        }
        return mask;
    }

    static AtlasCell hub(long seed, int hubX, int hubY) {
        if (hubX == 0 && hubY == 0) {
            return AtlasCell.ORIGIN;
        }
        int span = 2 * HUB_JITTER_STEPS + 1;
        int offsetX = Math.min(span - 1, (int) (AtlasHash.unit(AtlasHash.of(seed, HUB_X_SALT, hubX, hubY)) * span)) - HUB_JITTER_STEPS;
        int offsetY = Math.min(span - 1, (int) (AtlasHash.unit(AtlasHash.of(seed, HUB_Y_SALT, hubX, hubY)) * span)) - HUB_JITTER_STEPS;
        return new AtlasCell(hubX * HUB_SPACING + 2 * offsetX, hubY * HUB_SPACING + 2 * offsetY);
    }

    static List<AtlasCell> latticePath(AtlasCell from, AtlasCell to) {
        List<AtlasCell> path = new ArrayList<>(List.of(from));
        int x = from.gridX();
        int y = from.gridY();
        while (x != to.gridX() || y != to.gridY()) {
            int stepsX = (to.gridX() - x) / 2;
            int stepsY = (to.gridY() - y) / 2;
            if (2 * Math.abs(stepsX) >= Math.abs(stepsY)) {
                x += 2 * Integer.signum(stepsX);
            }
            if (2 * Math.abs(stepsY) >= Math.abs(stepsX)) {
                y += 2 * Integer.signum(stepsY);
            }
            path.add(new AtlasCell(x, y));
        }
        return path;
    }

    private Corridor corridor(List<AtlasCell> path) {
        float[] points = new float[path.size() * 2];
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int index = 0; index < path.size(); index++) {
            float x = positions.displayX(path.get(index));
            float y = positions.displayY(path.get(index));
            points[index * 2] = x;
            points[index * 2 + 1] = y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new Corridor(points, minX, minY, maxX, maxY);
    }

    private List<Corridor> corridorsFrom(int hubX, int hubY) {
        return hubCorridors.computeIfAbsent(new AtlasCell(hubX, hubY).pack(), key -> {
            AtlasCell hub = hub(seed, hubX, hubY);
            return List.of(corridor(latticePath(hub, hub(seed, hubX + 1, hubY))), corridor(latticePath(hub, hub(seed, hubX, hubY + 1))));
        });
    }

    private boolean inCorridor(float x, float y) {
        int hubX = Math.round(x / HUB_SPACING);
        int hubY = Math.round(y / HUB_SPACING);
        for (int nearX = hubX - 1; nearX <= hubX + 1; nearX++) {
            for (int nearY = hubY - 1; nearY <= hubY + 1; nearY++) {
                for (Corridor corridor : corridorsFrom(nearX, nearY)) {
                    if (corridor.clears(x, y)) {
                        return true;
                    }
                }
            }
        }
        for (Corridor spur : spurs) {
            if (spur.clears(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static float segmentSquared(float ax, float ay, float bx, float by, float px, float py) {
        float dx = bx - ax;
        float dy = by - ay;
        float lengthSquared = dx * dx + dy * dy;
        float along = lengthSquared <= 0f ? 0f : Math.max(0f, Math.min(1f, ((px - ax) * dx + (py - ay) * dy) / lengthSquared));
        return AtlasPositions.squared(ax + dx * along - px, ay + dy * along - py);
    }

    private boolean raw(float x, float y) {
        if (AtlasPositions.squared(x, y) < ORIGIN_CLEARANCE * ORIGIN_CLEARANCE || nearFixed(x, y) || inCorridor(x, y)) {
            return false;
        }
        AtlasBiomes.Border border = biomes.borderAt(x, y);
        AtlasRegion owner = region(border.owner());
        if (owner.clouds() > 0f && noise(CLUMP_SALT, x / CLUMP_NOISE_CELLS, y / CLUMP_NOISE_CELLS) > 1f - owner.clouds() * CLUMP_REACH) {
            return true;
        }
        AtlasRegion runnerUp = region(border.runnerUp());
        if (owner.id().equals(runnerUp.id()) || !walled(border, (owner.walls() + runnerUp.walls()) / 2f)) {
            return false;
        }
        float thickness = RIDGE_WIDTH * (noise(RIDGE_SALT, x / RIDGE_NOISE_CELLS, y / RIDGE_NOISE_CELLS) - PASS_SHARE) / (1f - PASS_SHARE);
        return border.gap() < thickness;
    }

    private boolean walled(AtlasBiomes.Border border, float chance) {
        long first = Math.min(border.owner().siteKey(), border.runnerUp().siteKey());
        long second = Math.max(border.owner().siteKey(), border.runnerUp().siteKey());
        AtlasCell low = AtlasCell.unpack(first);
        AtlasCell high = AtlasCell.unpack(second);
        long pair = AtlasHash.of(AtlasHash.of(seed, WALL_SALT, low.gridX(), low.gridY()), WALL_SALT, high.gridX(), high.gridY());
        return AtlasHash.unit(pair) < chance;
    }

    private boolean nearFixed(float x, float y) {
        for (float[] point : fixedPoints) {
            if (AtlasPositions.squared(point[0] - x, point[1] - y) < SYSTEM_CLEARANCE * SYSTEM_CLEARANCE) {
                return true;
            }
        }
        return false;
    }

    private AtlasRegion region(AtlasBiome biome) {
        AtlasRegion region = rules.region(biome.regionId());
        return region == null ? rules.normalRegion() : region;
    }

    private float noise(long salt, float x, float y) {
        return (valueNoise(salt, x, y) + 0.5f * valueNoise(salt ^ OCTAVE_SALT, x * 2f, y * 2f)) / 1.5f;
    }

    private float valueNoise(long salt, float x, float y) {
        int cornerX = (int) Math.floor(x);
        int cornerY = (int) Math.floor(y);
        float fractionX = smooth(x - cornerX);
        float fractionY = smooth(y - cornerY);
        float bottom = lerp(corner(salt, cornerX, cornerY), corner(salt, cornerX + 1, cornerY), fractionX);
        float top = lerp(corner(salt, cornerX, cornerY + 1), corner(salt, cornerX + 1, cornerY + 1), fractionX);
        return lerp(bottom, top, fractionY);
    }

    private float corner(long salt, int x, int y) {
        return (float) AtlasHash.unit(AtlasHash.of(seed, salt, x, y));
    }

    private static float smooth(float fraction) {
        return fraction * fraction * (3f - 2f * fraction);
    }

    private static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    private static float centre(int tile) {
        return (tile + 0.5f) / TILES_PER_CELL;
    }

    private static int bit(int localX, int localY) {
        return localX * TILES_PER_CELL + localY;
    }
}
