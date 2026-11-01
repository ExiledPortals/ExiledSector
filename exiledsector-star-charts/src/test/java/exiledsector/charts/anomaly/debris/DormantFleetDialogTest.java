package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartCampaignPlugin;
import exiledsector.items.AnomalousDebrisCurrencies;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ReservedModifiers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DormantFleetDialogTest {

    private static final String ENGRAM = "exiledSector_test_engram";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray().put(new JSONObject().put("id", "HULL_PERCENT_ANOMALOUS_DEBRIS").put("slot", "prefix")
                .put("item_types", "hull_item").put("lines", "HULL_PERCENT:15:15").put("name", "Revenant")));
        AnomalousDebrisCurrencies.register(new JSONArray().put(new JSONObject().put("id", ENGRAM).put("name", "Engram")
                .put("modifier", "HULL_PERCENT_ANOMALOUS_DEBRIS").put("item_types", "hull_item").put("min_tier", "1").put("weight", "1")));
    }

    @AfterEach
    void tearDown() {
        AnomalousDebrisCurrencies.unregisterAll();
        ReservedModifiers.clear();
        ItemModifiers.clear();
    }

    private static MemoryAPI memory() {
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        return memory;
    }

    private static CampaignFleetAPI debrisFleet(boolean corrupted) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MemoryAPI memory = memory();
        memory.set(DormantFleets.DORMANT_KEY, true);
        memory.set(DormantFleets.CORRUPTED_KEY, corrupted);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.hasTag(DormantFleets.FLEET_TAG)).thenReturn(true);
        return fleet;
    }

    @Test
    void corruptingIsGreyedOutWithoutAKernelAndEverythingWithoutReadyShips() {
        DormantFleetDialog.Options withKernel = DormantFleetDialog.options(true, 1);
        DormantFleetDialog.Options withoutKernel = DormantFleetDialog.options(true, 0);
        DormantFleetDialog.Options noShips = DormantFleetDialog.options(false, 5);

        assertTrue(withKernel.canEngage());
        assertTrue(withKernel.canCorrupt());
        assertNull(withKernel.corruptBlockedKey());
        assertTrue(withoutKernel.canEngage());
        assertFalse(withoutKernel.canCorrupt());
        assertEquals("chart.debris.dialog.noKernel", withoutKernel.corruptBlockedKey());
        assertFalse(noShips.canEngage());
        assertFalse(noShips.canCorrupt());
    }

    @Test
    void theCampaignPluginPicksTheDormantDialogForEveryDebrisFleet() {
        try (MockSector ignored = new MockSector()) {
            StarSystemAPI system = LiveAnomalyRuns.systemWith(AnomalousDebris.ANOMALY_ID);
            CampaignFleetAPI debris = LiveAnomalyRuns.placedIn(debrisFleet(false), system);

            PluginPick<InteractionDialogPlugin> pick = new ChartCampaignPlugin().pickInteractionDialogPlugin(debris);

            assertInstanceOf(DormantFleetDialog.class, pick.plugin);
            assertEquals(CampaignPlugin.PickPriority.MOD_SPECIFIC, pick.priority);
            assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(LiveAnomalyRuns.placedIn(mock(CampaignFleetAPI.class), system)));
        }
    }

    @Test
    void onlyACorruptedFleetDropsAnEngramAndOnlyOnce() {
        CampaignFleetAPI corrupted = debrisFleet(true);
        CampaignFleetAPI plain = debrisFleet(false);
        Random random = new Random(2L);

        ItemDrops.SiteLoot first = DormantFleetLoot.currencyFor(corrupted, 3, ChartGenContext.none(), random);
        assertEquals(Map.of(ENGRAM, 1), first.materials());
        assertTrue(DormantFleetLoot.currencyFor(corrupted, 3, ChartGenContext.none(), random).materials().isEmpty());
        assertTrue(DormantFleetLoot.currencyFor(plain, 3, ChartGenContext.none(), random).materials().isEmpty());
    }

    @Test
    void theQuantityLineRaisesTheEngramsDropped() {
        Random random = new Random(4L);
        assertEquals(2, DebrisRules.currencyCount(2f, random));
        int total = 0;
        for (int draw = 0; draw < 1000; draw++) {
            total += DebrisRules.currencyCount(1.5f, random);
        }
        assertTrue(total > 1400 && total < 1600, String.valueOf(total));
        assertEquals(1, DebrisRules.currencyCount(1f, random));
    }
}
