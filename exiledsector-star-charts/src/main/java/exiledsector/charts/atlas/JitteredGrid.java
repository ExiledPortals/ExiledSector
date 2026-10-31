package exiledsector.charts.atlas;

final class JitteredGrid {

    private static final long AXIS_SALT = 0x59L;

    private final long seed;
    private final long pointSalt;

    JitteredGrid(long seed, long pointSalt) {
        this.seed = seed;
        this.pointSalt = pointSalt;
    }

    float x(int gridX, int gridY) {
        return gridX + (float) AtlasHash.unit(AtlasHash.of(seed, pointSalt, gridX, gridY));
    }

    float y(int gridX, int gridY) {
        return gridY + (float) AtlasHash.unit(AtlasHash.of(seed, pointSalt ^ AXIS_SALT, gridX, gridY));
    }
}
