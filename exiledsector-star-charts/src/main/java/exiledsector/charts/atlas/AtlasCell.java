package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.List;

public record AtlasCell(int gridX, int gridY) {

    public static final AtlasCell ORIGIN = new AtlasCell(0, 0);
    public static final int CHUNK_SIZE = 8;

    public static AtlasCell unpack(long packed) {
        return new AtlasCell((int) (packed >> 32), (int) packed);
    }

    public static AtlasCell nearest(float mapX, float mapY) {
        return new AtlasCell(Math.round(mapX), Math.round(mapY));
    }

    public long pack() {
        return ((long) gridX << 32) | (gridY & 0xffffffffL);
    }

    public List<AtlasCell> around(int radius) {
        List<AtlasCell> cells = new ArrayList<>((2 * radius + 1) * (2 * radius + 1));
        for (int x = gridX - radius; x <= gridX + radius; x++) {
            for (int y = gridY - radius; y <= gridY + radius; y++) {
                cells.add(new AtlasCell(x, y));
            }
        }
        return cells;
    }

    public boolean mayBeVoid() {
        return ((gridX + gridY) & 1) != 0;
    }

    public int chunkX() {
        return Math.floorDiv(gridX, CHUNK_SIZE);
    }

    public int chunkY() {
        return Math.floorDiv(gridY, CHUNK_SIZE);
    }

    public float centreDistance() {
        return (float) Math.hypot(gridX, gridY);
    }
}
