package exiledsector.ui.socket;

record BenchSideBySide(float groupLeft, float groupTop, float groupHeight, float groupRight, float benchScale) {

    static BenchSideBySide in(SocketPanelPlacement area) {
        float benchScale = CraftingBenchPanel.scaleFor(area.panelWidth() - ItemStoragePanel.COMPACT_WIDTH - CraftingBench.PANEL_GAP, area.panelHeight());
        float benchWidth = CraftingBenchPanel.sizeFor(benchScale, BenchScene.ART_WIDTH);
        float benchHeight = CraftingBenchPanel.sizeFor(benchScale, BenchScene.ART_HEIGHT);
        float groupWidth = ItemStoragePanel.COMPACT_WIDTH + CraftingBench.PANEL_GAP + benchWidth;
        float groupLeft = area.panelLeft() + (area.panelWidth() - groupWidth) / 2f;
        float groupTop = area.panelTop() + (area.panelHeight() - benchHeight) / 2f;
        return new BenchSideBySide(groupLeft, groupTop, benchHeight, groupLeft + groupWidth, benchScale);
    }

    float fullBenchWidth() {
        return CraftingBenchPanel.sizeFor(benchScale, BenchScene.ART_WIDTH);
    }

    float fullBenchLeft() {
        return groupRight - fullBenchWidth();
    }

    static float storageLeft(float benchLeft) {
        return benchLeft - CraftingBench.PANEL_GAP - ItemStoragePanel.COMPACT_WIDTH;
    }

    FreeArea searchArea(float benchLeft) {
        float searchRight = storageLeft(benchLeft) - CraftingBench.PANEL_GAP;
        return new FreeArea(groupLeft, groupTop, searchRight - groupLeft, groupHeight);
    }
}
