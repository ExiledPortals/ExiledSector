package exiledsector.charts.atlas;

import java.util.LinkedHashMap;
import java.util.Map;

final class AtlasPositions {

    static final float JITTER_REACH = 0.4f;
    private static final long JITTER_X_SALT = 0x4a4954585fL;
    private static final long JITTER_Y_SALT = 0x4a4954595fL;

    private final long seed;

    AtlasPositions(long seed) {
        this.seed = seed;
    }

    float mapX(AtlasCell cell) {
        return cell.gridX() + jitter(cell, JITTER_X_SALT);
    }

    float mapY(AtlasCell cell) {
        return cell.gridY() + jitter(cell, JITTER_Y_SALT);
    }

    private float jitter(AtlasCell cell, long salt) {
        if (cell.equals(AtlasCell.ORIGIN)) {
            return 0f;
        }
        return (float) (AtlasHash.unit(AtlasHash.of(seed, salt, cell.gridX(), cell.gridY())) - 0.5) * 2f * JITTER_REACH;
    }

    static boolean isLattice(AtlasCell cell) {
        return ((cell.gridX() | cell.gridY()) & 1) == 0;
    }

    static float squared(float dx, float dy) {
        return dx * dx + dy * dy;
    }

    static <V> Map<Long, V> boundedCache(int limit) {
        return new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > limit;
            }
        };
    }
}
