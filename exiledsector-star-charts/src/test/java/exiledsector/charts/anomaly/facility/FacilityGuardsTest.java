package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.loot.ChartFleetLedger;
import exiledsector.charts.run.ChartRun;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static exiledsector.charts.anomaly.LiveAnomalyRuns.RUN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FacilityGuardsTest {

    private static final float GUARD_DEPLOYMENT_POINTS = 30f;

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        MemoryAPI globalMemory = FakeChartContentEngine.memory();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(globalMemory);
        FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing().withShips(GUARD_DEPLOYMENT_POINTS));
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        lunaSettingsMock.close();
        mockSector.close();
    }

    @Test
    void theGuardsDropItemsLikeAnyOtherChartFleet() {
        ChartRun run = LiveAnomalyRuns.run(4);

        CampaignFleetAPI guards = FacilityGuards.create(run, 7L);

        MemoryAPI guardMemory = guards.getMemoryWithoutUpdate();
        assertTrue(ChartFleetLedger.hasLootLedger(guards));
        assertEquals(GUARD_DEPLOYMENT_POINTS, guardMemory.getFloat(ChartIds.START_DP_MEMORY_KEY));
        assertTrue(guardMemory.getBoolean(ChartIds.fleetRunKey(RUN_ID)));
        assertEquals(4, guardMemory.get(ChartIds.TIER_MEMORY_KEY));
    }
}
