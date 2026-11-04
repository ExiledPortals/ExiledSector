package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchSideBySideTest {

    private static final float TOLERANCE = 0.001f;

    private static BenchSideBySide benchOn(float screenWidth, float screenHeight) {
        return BenchSideBySide.in(new SocketPanelPlacement(40f, 40f, screenWidth - 80f, screenHeight - 80f, false));
    }

    @Test
    void openingTheSearchCompactsTheBenchByOneStepAndKeepsItsRightEdge() {
        BenchSideBySide bench = benchOn(1920f, 1080f);
        BenchSideBySide.Placed closed = bench.placed(false);
        BenchSideBySide.Placed open = bench.placed(true);

        assertEquals(closed.benchScale() - 1f, open.benchScale(), TOLERANCE);
        assertEquals(closed.benchLeft() + closed.benchWidth(), open.benchLeft() + open.benchWidth(), TOLERANCE);
        assertEquals(closed.benchTop() + closed.benchHeight() / 2f, open.benchTop() + open.benchHeight() / 2f, TOLERANCE,
                "the compacted bench stays centred on the same line");
        assertEquals(open.benchLeft() - CraftingBench.PANEL_GAP - ItemStoragePanel.COMPACT_WIDTH, open.storageLeft(), TOLERANCE,
                "the hull items panel moves in with the bench");
    }

    @Test
    void theSearchTakesTheHullItemsPanelsLeftEdgeAndHeight() {
        BenchSideBySide bench = benchOn(1920f, 1080f);
        FreeArea search = bench.searchArea();

        assertEquals(bench.placed(false).storageLeft(), search.left(), TOLERANCE);
        assertEquals(bench.groupTop(), search.top(), TOLERANCE);
        assertEquals(bench.groupHeight(), search.height(), TOLERANCE);
        assertEquals(bench.placed(true).storageLeft() - CraftingBench.PANEL_GAP, search.right(), TOLERANCE);
        assertTrue(search.width() > 250f && search.width() < ItemStoragePanel.COMPACT_WIDTH, "width " + search.width());
    }

    @Test
    void theSearchKeepsTheSameWidthAtLargerScreens() {
        assertEquals(benchOn(1920f, 1080f).searchArea().width(), benchOn(2560f, 1440f).searchArea().width(), TOLERANCE);
    }
}
