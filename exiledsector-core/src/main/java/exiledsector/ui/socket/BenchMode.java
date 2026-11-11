package exiledsector.ui.socket;

public enum BenchMode {
    HULL_ITEMS(null, 160f, 83.3f, 58f, 42f),
    STAR_CHARTS(null, 160f, 70f, 58f, 34f),
    FORGE("graphics/ui/exiledSector/derelict_forge.png", 165f, 88f, 58f, 42f),
    ABYSSAL_FORGE("graphics/ui/exiledSector/abyssal_research_complex.png", 160f, 88f, 58f, 42f);

    private final String artPath;
    private final float socketX;
    private final float socketY;
    private final float orbitRadiusX;
    private final float orbitRadiusY;

    BenchMode(String artPath, float socketX, float socketY, float orbitRadiusX, float orbitRadiusY) {
        this.artPath = artPath;
        this.socketX = socketX;
        this.socketY = socketY;
        this.orbitRadiusX = orbitRadiusX;
        this.orbitRadiusY = orbitRadiusY;
    }

    String artPath() {
        return artPath;
    }

    boolean drawsStarfield() {
        return artPath == null;
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
}
