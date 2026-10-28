package exiledsector.ui.socket;

public enum BenchMode {
    HULL_ITEMS("graphics/ui/exiledSector/modification_bench.png", 159f, 83.3f, 58f, 42f, 68.5f, 112f, 243.5f, 112f),
    STAR_CHARTS("graphics/ui/exiledSector/map_device.png", 169.7f, 70f, 58f, 34f, 37.5f, 103.5f, 283.7f, 100.7f),
    FORGE("graphics/ui/exiledSector/derelict_forge.png", 165f, 88f, 58f, 42f, 68.5f, 112f, 243.5f, 112f);

    private final String artPath;
    private final float socketX;
    private final float socketY;
    private final float orbitRadiusX;
    private final float orbitRadiusY;
    private final float leftConsoleX;
    private final float leftConsoleY;
    private final float rightConsoleX;
    private final float rightConsoleY;

    BenchMode(String artPath, float socketX, float socketY, float orbitRadiusX, float orbitRadiusY, float leftConsoleX, float leftConsoleY,
              float rightConsoleX, float rightConsoleY) {
        this.artPath = artPath;
        this.socketX = socketX;
        this.socketY = socketY;
        this.orbitRadiusX = orbitRadiusX;
        this.orbitRadiusY = orbitRadiusY;
        this.leftConsoleX = leftConsoleX;
        this.leftConsoleY = leftConsoleY;
        this.rightConsoleX = rightConsoleX;
        this.rightConsoleY = rightConsoleY;
    }

    String artPath() {
        return artPath;
    }

    float socketX() {
        return socketX;
    }

    float socketY() {
        return socketY;
    }

    float orbitRadiusX() {
        return orbitRadiusX;
    }

    float orbitRadiusY() {
        return orbitRadiusY;
    }

    float leftConsoleX() {
        return leftConsoleX;
    }

    float leftConsoleY() {
        return leftConsoleY;
    }

    float rightConsoleX() {
        return rightConsoleX;
    }

    float rightConsoleY() {
        return rightConsoleY;
    }
}
