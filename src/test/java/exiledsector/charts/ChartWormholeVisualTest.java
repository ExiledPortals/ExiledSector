package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartWormholeVisualTest {

    private static final String RUN_ID = "abc";
    private static final String ORIGIN_SYSTEM_ID = "corvus";

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        LocationAPI hyperspace = mock(LocationAPI.class);
        when(hyperspace.isHyperspace()).thenReturn(true);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getStarSystem(ORIGIN_SYSTEM_ID)).thenReturn(mock(StarSystemAPI.class));
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static SectorEntityToken wormhole(String roleTag) {
        SectorEntityToken wormhole = mock(SectorEntityToken.class);
        when(wormhole.hasTag(ChartIds.ENTITY_TAG)).thenReturn(true);
        when(wormhole.hasTag(roleTag)).thenReturn(true);
        when(wormhole.hasTag(ChartIds.runTag(RUN_ID))).thenReturn(true);
        return wormhole;
    }

    private static void openRunFrom(String originLocationId) {
        ChartRuns.replace(new ChartRun(RUN_ID, originLocationId, 0f, 0f, 1));
    }

    @Test
    void theOuterWormholeShowsTheStarfieldOfTheSystemItLeadsInto() {
        openRunFrom(ChartIds.HYPERSPACE_LOCATION_ID);

        assertEquals(ChartWormholeVisual.STARFIELD_BACKDROP, ChartWormholeVisual.backdropFor(sector, wormhole(ChartIds.OUTER_WORMHOLE_TAG)));
    }

    @Test
    void theInnerWormholeShowsHyperspaceWhenTheRunWasOpenedThere() {
        openRunFrom(ChartIds.HYPERSPACE_LOCATION_ID);

        assertEquals(ChartWormholeVisual.HYPERSPACE_BACKDROP, ChartWormholeVisual.backdropFor(sector, wormhole(ChartIds.INNER_WORMHOLE_TAG)));
    }

    @Test
    void theInnerWormholeShowsTheStarfieldWhenTheRunWasOpenedInAStarSystem() {
        openRunFrom(ORIGIN_SYSTEM_ID);

        assertEquals(ChartWormholeVisual.STARFIELD_BACKDROP, ChartWormholeVisual.backdropFor(sector, wormhole(ChartIds.INNER_WORMHOLE_TAG)));
    }

    @Test
    void anInnerWormholeWithoutALiveRunShowsTheStarfield() {
        assertEquals(ChartWormholeVisual.STARFIELD_BACKDROP, ChartWormholeVisual.backdropFor(sector, wormhole(ChartIds.INNER_WORMHOLE_TAG)));
    }

    @Test
    void aNewWormholeStartsOpenAndALoadedOneInAStarSystemStartsClosedSoItNeverCollapsesAgainOnLoad() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        LocationAPI hyperspace = sector.getHyperspace();
        SectorEntityToken inSystem = wormhole(ChartIds.INNER_WORMHOLE_TAG);
        when(inSystem.getContainingLocation()).thenReturn(system);
        SectorEntityToken inHyperspace = wormhole(ChartIds.OUTER_WORMHOLE_TAG);
        when(inHyperspace.getContainingLocation()).thenReturn(hyperspace);

        assertTrue(ChartWormholeVisual.startsOpen(inSystem, true));
        assertFalse(ChartWormholeVisual.startsOpen(inSystem, false));
        assertTrue(ChartWormholeVisual.startsOpen(inHyperspace, false));
    }

    @Test
    void aClosedWormholeShrinksToAQuarterLikeAVanillaWormhole() {
        assertEquals(0.25f, ChartWormholeVisual.scaleFor(0f), 1e-6f);
        assertEquals(1f, ChartWormholeVisual.scaleFor(1f), 1e-6f);
        assertEquals(0.25f + 0.75f * 0.25f, ChartWormholeVisual.scaleFor(0.5f), 1e-6f);
    }

    @Test
    void fadesWithAbyssalDepthLikeAVanillaJumpPoint() {
        assertEquals(1f, ChartWormholeVisual.abyssalAlphaMult(0f, 0f, 0.3f), 1e-6f);
        assertEquals(0.6f, ChartWormholeVisual.abyssalAlphaMult(0.4f, 0f, 0.3f), 1e-6f);
        assertEquals(0.3f, ChartWormholeVisual.abyssalAlphaMult(0.4f, 0.2f, 0.3f), 1e-6f);
    }

    @Test
    void bandFluctuationsBounceWithinTheirLimit() {
        ChartWormholeBand.Fluctuation fluctuation = new ChartWormholeBand.Fluctuation(2f);
        float lowest = 0f;
        float highest = 0f;
        for (int step = 0; step < 2000; step++) {
            fluctuation.advance(0.05f);
            lowest = Math.min(lowest, fluctuation.value());
            highest = Math.max(highest, fluctuation.value());
        }

        assertTrue(lowest < -1.5f && lowest >= -2f - 0.05f * 4f, "lowest " + lowest);
        assertTrue(highest > 1.5f && highest <= 2f + 0.05f * 4f, "highest " + highest);
    }

    @Test
    void theBandSplitsItsCircleIntoSegmentsOfTheGivenLength() {
        ChartWormholeBand band = new ChartWormholeBand(mock(SpriteAPI.class), 90f, 10f, 50f, 5f, 0.5f);

        assertEquals(Math.round(Math.PI * 2 * 50f / 10f), band.segmentCount());
    }

    @Test
    void ringsWobbleByAQuarterOfTheirSize() {
        ChartWormholeVisual.Drift drift = new ChartWormholeVisual.Drift(1f);
        for (int step = 0; step < 500; step++) {
            drift.advance(0.013f);

            assertTrue(drift.xScale >= 0.75f - 1e-4f && drift.xScale <= 1.25f + 1e-4f);
            assertEquals(2f, drift.xScale + drift.yScale, 1e-4f);
        }
    }
}
