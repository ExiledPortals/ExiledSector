package exiledsector.ui.socket;

import exiledsector.i18n.Translation;
import exiledsector.items.ItemStore;

public enum BenchMode {
    HULL_ITEMS("graphics/ui/exiledSector/modification_bench.png", 159f, 83.3f, 58f, 42f, true, "ui.socketPanel.tab.socketables"),
    STAR_CHARTS("graphics/ui/exiledSector/map_device.png", 169.7f, 70f, 58f, 34f, false, "ui.socketPanel.tab.starCharts");

    private final String artPath;
    private final float socketX;
    private final float socketY;
    private final float orbitRadiusX;
    private final float orbitRadiusY;
    private final boolean showsInhibitors;
    private final String labelKey;

    BenchMode(String artPath, float socketX, float socketY, float orbitRadiusX, float orbitRadiusY, boolean showsInhibitors, String labelKey) {
        this.artPath = artPath;
        this.socketX = socketX;
        this.socketY = socketY;
        this.orbitRadiusX = orbitRadiusX;
        this.orbitRadiusY = orbitRadiusY;
        this.showsInhibitors = showsInhibitors;
        this.labelKey = labelKey;
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

    boolean showsInhibitors() {
        return showsInhibitors;
    }

    String label() {
        return Translation.text(labelKey);
    }

    ItemStore store() {
        return this == STAR_CHARTS ? ItemStore.starChartsIfCreated() : ItemStore.get();
    }
}
