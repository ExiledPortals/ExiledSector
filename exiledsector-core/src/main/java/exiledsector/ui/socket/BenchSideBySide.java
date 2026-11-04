package exiledsector.ui.socket;

record BenchSideBySide(float groupLeft, float groupTop, float groupHeight, float groupRight, float fullScale) {

    record Placed(float storageLeft, float benchLeft, float benchTop, float benchWidth, float benchHeight, float benchScale) {
    }

    static BenchSideBySide in(SocketPanelPlacement area) {
        float fullScale = CraftingBenchPanel.scaleFor(area.panelWidth() - ItemStoragePanel.COMPACT_WIDTH - CraftingBench.PANEL_GAP, area.panelHeight());
        float benchWidth = CraftingBenchPanel.sizeFor(fullScale, BenchScene.ART_WIDTH);
        float benchHeight = CraftingBenchPanel.sizeFor(fullScale, BenchScene.ART_HEIGHT);
        float groupWidth = ItemStoragePanel.COMPACT_WIDTH + CraftingBench.PANEL_GAP + benchWidth;
        float groupLeft = area.panelLeft() + (area.panelWidth() - groupWidth) / 2f;
        float groupTop = area.panelTop() + (area.panelHeight() - benchHeight) / 2f;
        return new BenchSideBySide(groupLeft, groupTop, benchHeight, groupLeft + groupWidth, fullScale);
    }

    Placed placed(boolean searchOpen) {
        float benchScale = searchOpen ? Math.max(BenchScene.MIN_WHOLE_SCALE, fullScale - 1f) : fullScale;
        float benchWidth = CraftingBenchPanel.sizeFor(benchScale, BenchScene.ART_WIDTH);
        float benchHeight = CraftingBenchPanel.sizeFor(benchScale, BenchScene.ART_HEIGHT);
        float benchLeft = groupRight - benchWidth;
        float benchTop = groupTop + (groupHeight - benchHeight) / 2f;
        float storageLeft = benchLeft - CraftingBench.PANEL_GAP - ItemStoragePanel.COMPACT_WIDTH;
        return new Placed(storageLeft, benchLeft, benchTop, benchWidth, benchHeight, benchScale);
    }

    FreeArea searchArea() {
        float searchRight = placed(true).storageLeft() - CraftingBench.PANEL_GAP;
        return new FreeArea(groupLeft, groupTop, searchRight - groupLeft, groupHeight);
    }
}
