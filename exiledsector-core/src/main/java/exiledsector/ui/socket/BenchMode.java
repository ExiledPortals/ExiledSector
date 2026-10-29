package exiledsector.ui.socket;

public enum BenchMode {
    HULL_ITEMS(null, 160f, 83.3f, 58f, 42f, 100f),
    STAR_CHARTS(null, 160f, 70f, 58f, 34f, 100f),
    FORGE("graphics/ui/exiledSector/derelict_forge.png", 165f, 88f, 58f, 42f, 100f);

    private final String artPath;
    private final float socketX;
    private final float socketY;
    private final float orbitRadiusX;
    private final float orbitRadiusY;
    private final float wardOffsetX;

    BenchMode(String artPath, float socketX, float socketY, float orbitRadiusX, float orbitRadiusY, float wardOffsetX) {
        this.artPath = artPath;
        this.socketX = socketX;
        this.socketY = socketY;
        this.orbitRadiusX = orbitRadiusX;
        this.orbitRadiusY = orbitRadiusY;
        this.wardOffsetX = wardOffsetX;
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

    float leftWardX() {
        return socketX - wardOffsetX;
    }

    float rightWardX() {
        return socketX + wardOffsetX;
    }

    float wardY() {
        return socketY;
    }
}
