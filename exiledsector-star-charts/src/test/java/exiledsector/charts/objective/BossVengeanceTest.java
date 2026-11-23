package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartFleetMarks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BossVengeanceTest {

    private static final String RUN_ID = "run1";

    private final List<CampaignFleetAPI> fleets = new ArrayList<>();
    private StarSystemAPI system;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(fleets);
        when(system.createToken(anyFloat(), anyFloat())).thenReturn(mock(SectorEntityToken.class));
    }

    static CampaignFleetAPI fleet(String runId) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getAI()).thenReturn(mock(CampaignFleetAIAPI.class));
        when(fleet.hasTag(ChartIds.runTag(runId))).thenReturn(true);
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        return fleet;
    }

    private CampaignFleetAPI fleetHere(String runId) {
        CampaignFleetAPI fleet = fleet(runId);
        fleets.add(fleet);
        return fleet;
    }

    @Test
    void theRunsOwnFleetsSwearVengeanceAndTurnAggressive() {
        CampaignFleetAPI patrol = fleetHere(RUN_ID);
        CampaignFleetAPI twin = fleetHere(RUN_ID);

        assertTrue(BossVengeance.begin(system, RUN_ID, new Vector2f(100f, 200f)));

        for (CampaignFleetAPI avenger : List.of(patrol, twin)) {
            assertTrue(ChartFleetMarks.isAvenger(avenger));
            assertTrue(avenger.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE));
        }
        verify(system).createToken(100f, 200f);
        verify(system).addScript(any(VengeanceScript.class));
    }

    @Test
    void anomalyFleetsStationsThePlayerAndOtherRunsStayOut() {
        CampaignFleetAPI anomaly = fleetHere(RUN_ID);
        ChartFleetMarks.markAnomalyFleet(anomaly);
        CampaignFleetAPI station = fleetHere(RUN_ID);
        when(station.isStationMode()).thenReturn(true);
        CampaignFleetAPI player = fleetHere(RUN_ID);
        when(player.isPlayerFleet()).thenReturn(true);
        CampaignFleetAPI stranger = fleetHere("otherRun");
        when(stranger.hasTag(ChartIds.runTag(RUN_ID))).thenReturn(false);
        CampaignFleetAPI wreck = fleetHere(RUN_ID);
        when(wreck.isEmpty()).thenReturn(true);

        assertFalse(BossVengeance.begin(system, RUN_ID, new Vector2f()));

        for (CampaignFleetAPI fleet : List.of(anomaly, station, player, stranger, wreck)) {
            assertFalse(ChartFleetMarks.isAvenger(fleet));
        }
        verify(system, never()).addScript(any());
    }

    @Test
    void theRallyPointIsWhereTheBossFellOrElseWhereThePlayerIs() {
        CampaignFleetAPI boss = fleet(RUN_ID);
        when(boss.getContainingLocation()).thenReturn(system);
        when(boss.getLocation()).thenReturn(new Vector2f(5f, 6f));
        CampaignFleetAPI player = fleet(RUN_ID);
        when(player.getContainingLocation()).thenReturn(system);
        when(player.getLocation()).thenReturn(new Vector2f(-1f, -2f));
        CampaignFleetAPI bossElsewhere = fleet(RUN_ID);
        when(bossElsewhere.getContainingLocation()).thenReturn(mock(StarSystemAPI.class));
        when(bossElsewhere.getLocation()).thenReturn(new Vector2f(9f, 9f));

        assertEquals(new Vector2f(5f, 6f), BossVengeance.bossAt(system, boss, player));
        assertEquals(new Vector2f(-1f, -2f), BossVengeance.bossAt(system, null, player));
        assertEquals(new Vector2f(-1f, -2f), BossVengeance.bossAt(system, bossElsewhere, player));
        assertNull(BossVengeance.bossAt(system, null, null));
        assertFalse(BossVengeance.begin(system, RUN_ID, null));
    }
}
