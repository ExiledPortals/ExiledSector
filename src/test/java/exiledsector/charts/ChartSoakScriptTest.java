package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartSoakScriptTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CampaignFleetAPI playerFleet;
    private final List<StarSystemAPI> starSystems = new ArrayList<>();
    private final List<StarSystemAPI> removeNextFrame = new ArrayList<>();
    private final List<String> reports = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        LocationAPI hyperspace = mock(LocationAPI.class);
        when(hyperspace.isHyperspace()).thenReturn(true);
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        playerFleet = mock(CampaignFleetAPI.class);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getLocation()).thenReturn(new Vector2f());
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(sector.getStarSystems()).thenAnswer(invocation -> new ArrayList<>(starSystems));
        when(sector.createStarSystem(anyString())).thenAnswer(invocation -> newPocket());
        when(sector.getStarSystem(anyString())).thenAnswer(invocation -> starSystems.stream()
                .filter(system -> invocation.getArgument(0).equals(system.getOptionalUniqueId())).findFirst().orElse(null));
        doAnswer(invocation -> removeNextFrame.add(invocation.getArgument(0))).when(sector).removeStarSystemNextFrame(Mockito.any());
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private StarSystemAPI newPocket() {
        StarSystemAPI pocket = mock(StarSystemAPI.class);
        String[] uniqueId = new String[1];
        doAnswer(invocation -> uniqueId[0] = invocation.getArgument(0)).when(pocket).setOptionalUniqueId(anyString());
        when(pocket.getOptionalUniqueId()).thenAnswer(invocation -> uniqueId[0]);
        when(pocket.getLocation()).thenReturn(new Vector2f());
        when(pocket.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        when(pocket.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
        when(pocket.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        starSystems.add(pocket);
        return pocket;
    }

    private void runFrames(ChartSoakScript soak, ChartRunScript watchdog, int frames) {
        for (int frame = 0; frame < frames && !soak.isDone(); frame++) {
            starSystems.removeAll(removeNextFrame);
            removeNextFrame.clear();
            soak.advance(0f);
            watchdog.advance(0f);
        }
    }

    @Test
    void soaksTheRequestedPocketsWithoutMovingThePlayerAndReportsBeforeAndAfter() {
        ChartCensus before = ChartCensus.take(sector);
        ChartSoakScript soak = new ChartSoakScript(3, before, reports::add, new Random(7));

        runFrames(soak, new ChartRunScript(), 100);

        assertTrue(soak.isDone());
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).contains("finished, 3 of 3"), reports.get(0));
        assertTrue(starSystems.isEmpty());
        assertNull(ChartRuns.currentOrNull());
        verify(sector, Mockito.times(3)).createStarSystem(ChartIds.POCKET_BASE_NAME);
        verify(sector, never()).doHyperspaceTransition(Mockito.any(), Mockito.any(), Mockito.any());
        verify(playerFleet, never()).setLocation(anyFloat(), anyFloat());
    }

    @Test
    void stopsAndReportsWhenARunIsAlreadyOpen() {
        ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        ChartSoakScript soak = new ChartSoakScript(2, ChartCensus.take(sector), reports::add, new Random(7));

        soak.advance(0f);

        assertTrue(soak.isDone());
        assertTrue(reports.get(0).contains("RUN_EXISTS"), reports.get(0));
    }
}
