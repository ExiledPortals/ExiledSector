package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BenchSideBySideTest {

    private static final float TOLERANCE = 0.001f;

    private static BenchSideBySide benchOn(float screenWidth, float screenHeight) {
        return BenchSideBySide.in(new SocketPanelPlacement(40f, 40f, screenWidth - 80f, screenHeight - 80f, false));
    }

    @Test
    void theFullBenchEndsAtTheGroupsRightEdgeWithTheHullItemsPanelBesideIt() {
        BenchSideBySide bench = benchOn(1920f, 1080f);

        assertEquals(bench.groupRight(), bench.fullBenchLeft() + bench.fullBenchWidth(), TOLERANCE);
        assertEquals(bench.groupLeft(), BenchSideBySide.storageLeft(bench.fullBenchLeft()), TOLERANCE);
    }

    @Test
    void theSearchTakesEverythingTheBenchGaveUpAtFullHeight() {
        BenchSideBySide bench = benchOn(1920f, 1080f);
        float contractedWidth = 600f;
        float benchLeft = bench.groupRight() - contractedWidth;
        FreeArea search = bench.searchArea(benchLeft);

        assertEquals(bench.groupLeft(), search.left(), TOLERANCE);
        assertEquals(bench.groupTop(), search.top(), TOLERANCE);
        assertEquals(bench.groupHeight(), search.height(), TOLERANCE);
        assertEquals(bench.fullBenchWidth() - contractedWidth - CraftingBench.PANEL_GAP, search.width(), TOLERANCE);
    }
}
