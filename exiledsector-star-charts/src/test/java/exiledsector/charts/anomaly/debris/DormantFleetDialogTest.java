package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartCampaignPlugin;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.Engrams;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ModifierSources;
import exiledsector.loot.Loot;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DormantFleetDialogTest {

    private static final String ENGRAM = "exiledSector_test_engram";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray().put(new JSONObject().put("id", "HULL_PERCENT_ANOMALOUS_DEBRIS").put("affix", "prefix")
                .put("scopes", "hull_item").put("lines", "HULL_PERCENT:15:15").put("name", "Revenant")));
        Engrams.register(new JSONArray().put(new JSONObject().put("id", ENGRAM).put("name", "Engram")
                .put("modifier", "HULL_PERCENT_ANOMALOUS_DEBRIS").put("item_types", "hull_item").put("min_tier", "1").put("weight", "1")));
    }

    @AfterEach
    void tearDown() {
        Engrams.unregisterAll();
        ModifierSources.clear();
        ItemModifiers.clear();
    }

    private static MemoryAPI memory() {
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        doAnswer(invocation -> stored.remove((String) invocation.getArgument(0))).when(memory).unset(anyString());
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        when(memory.contains(anyString())).thenAnswer(invocation -> stored.containsKey((String) invocation.getArgument(0)));
        when(memory.getInt(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)) instanceof Integer count ? count : 0);
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
        assertTrue(withKernel.corruption(1).enabled());
        assertNull(withKernel.corruption(1).blockedKey());
        assertNull(withKernel.corruption(2));
        assertTrue(withoutKernel.canEngage());
        assertFalse(withoutKernel.corruption(1).enabled());
        assertEquals("chart.anomaly.anomalous_debris.dialog.noKernel", withoutKernel.corruption(1).blockedKey());
        assertFalse(noShips.canEngage());
        assertFalse(noShips.corruption(1).enabled());
        assertEquals("chart.anomaly.anomalous_debris.dialog.noReadyShips", noShips.corruption(1).blockedKey());
    }

    @Test
    void aSecondCorruptionIsOfferedForAnotherNanoswarmOnlyWhileOneIsLeft() {
        DormantFleetDialog.Options twoLeft = DormantFleetDialog.options(true, 2, 2);
        DormantFleetDialog.Options oneHeld = DormantFleetDialog.options(true, 1, 2);
        DormantFleetDialog.Options noneLeft = DormantFleetDialog.options(true, 5, 0);

        assertEquals(2, twoLeft.corruption(2).cost());
        assertTrue(twoLeft.corruption(1).enabled());
        assertTrue(twoLeft.corruption(2).enabled());
        assertTrue(oneHeld.corruption(1).enabled());
        assertFalse(oneHeld.corruption(2).enabled());
        assertEquals("chart.anomaly.anomalous_debris.dialog.notEnoughKernels", oneHeld.corruption(2).blockedKey());
        assertTrue(noneLeft.corruptions().isEmpty());
        assertTrue(noneLeft.canEngage());
        assertFalse(twoLeft.corruption(1).optionId().equals(twoLeft.corruption(2).optionId()));
    }

    private static CampaignFleetAPI corruptedTimes(int corruptions) {
        CampaignFleetAPI fleet = debrisFleet(true);
        fleet.getMemoryWithoutUpdate().set(DormantFleets.CORRUPTIONS_KEY, corruptions);
        return fleet;
    }

    @Test
    void theEngramLinesAddToWhatACorruptedFleetDrops() {
        ChartGenContext deepAndDouble = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS, 1f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_TWICE_CORRUPTED_ENGRAMS, 2f)));

        assertEquals(Map.of(ENGRAM, 2), DormantFleetLoot.craftingItemFor(corruptedTimes(1), 3, deepAndDouble, new Random(1L)).materials());
        assertEquals(Map.of(ENGRAM, 4), DormantFleetLoot.craftingItemFor(corruptedTimes(2), 3, deepAndDouble, new Random(1L)).materials());
        assertEquals(Map.of(ENGRAM, 1), DormantFleetLoot.craftingItemFor(corruptedTimes(2), 3, ChartGenContext.none(), new Random(1L)).materials());
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

    private static MockedConstruction<FleetInteractionDialogPluginImpl> encountersIn(MockSector mockSector) {
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        return mockConstruction(FleetInteractionDialogPluginImpl.class);
    }

    @Test
    void aFleetWokenFromTheDialogStaysAtItsFieldEvenWhenRestlessSleepersMakeTheOthersRoam() {
        try (MockSector mockSector = new MockSector();
             MockedStatic<LunaSettings> ignored = mockStatic(LunaSettings.class);
             MockedConstruction<FleetInteractionDialogPluginImpl> encounters = encountersIn(mockSector)) {
            FactoryAPI factory = mock(FactoryAPI.class);
            mockSector.global.when(Global::getFactory).thenReturn(factory);
            when(factory.createFleetAI(any())).thenReturn(mock(CampaignFleetAIAPI.class));
            MemoryAPI sectorMemory = memory();
            when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
            when(mockSector.sector.getCharacterData()).thenReturn(mock(CharacterDataAPI.class));
            StarSystemAPI system = LiveAnomalyRuns.systemWith(AnomalousDebris.ANOMALY_ID);
            when(system.getCenter()).thenReturn(mock(SectorEntityToken.class));
            when(system.createToken(anyFloat(), anyFloat())).thenReturn(mock(SectorEntityToken.class));
            ChartRuns.currentOrNull().recordAtlasTreeLines(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_AWAKE, 1f),
                    new ChartGenContext.AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_ROAMING, 1f)));
            CampaignFleetAPI dormant = LiveAnomalyRuns.placedIn(debrisFleet(false), system);
            when(dormant.getLocation()).thenReturn(new Vector2f(100f, 200f));
            when(dormant.getDetectedRangeMod()).thenReturn(new StatBonus());
            FleetDataAPI fleetData = mock(FleetDataAPI.class);
            when(fleetData.getCombatReadyMembersListCopy()).thenReturn(List.of(mock(FleetMemberAPI.class)));
            when(mockSector.playerFleet().getFleetData()).thenReturn(fleetData);
            mockSector.cargo();
            InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, RETURNS_DEEP_STUBS);
            when(dialog.getInteractionTarget()).thenReturn(dormant);
            DormantFleetDialog plugin = new DormantFleetDialog();

            plugin.init(dialog);
            plugin.optionSelected(null, DormantFleetDialog.ENGAGE_OPTION);

            assertFalse(DormantFleets.isDormant(dormant));
            verify(dormant).addAssignment(eq(FleetAssignment.ORBIT_AGGRESSIVE), any(SectorEntityToken.class), anyFloat());
            verify(dormant, never()).addAssignment(eq(FleetAssignment.PATROL_SYSTEM), any(SectorEntityToken.class), anyFloat());
            verify(encounters.constructed().get(0)).init(dialog);
        }
    }

    @Test
    void onlyACorruptedFleetDropsAnEngramAndOnlyOnce() {
        CampaignFleetAPI corrupted = debrisFleet(true);
        CampaignFleetAPI plain = debrisFleet(false);
        Random random = new Random(2L);

        Loot first = DormantFleetLoot.craftingItemFor(corrupted, 3, ChartGenContext.none(), random);
        assertEquals(Map.of(ENGRAM, 1), first.materials());
        assertTrue(DormantFleetLoot.craftingItemFor(corrupted, 3, ChartGenContext.none(), random).materials().isEmpty());
        assertTrue(DormantFleetLoot.craftingItemFor(plain, 3, ChartGenContext.none(), random).materials().isEmpty());
    }

    @Test
    void theQuantityLineRaisesTheEngramsDropped() {
        Random random = new Random(4L);
        assertEquals(2, DebrisRules.craftingItemCount(1, 2f, random));
        int total = 0;
        for (int draw = 0; draw < 1000; draw++) {
            total += DebrisRules.craftingItemCount(1, 1.5f, random);
        }
        assertTrue(total > 1400 && total < 1600, String.valueOf(total));
        assertEquals(1, DebrisRules.craftingItemCount(1, 1f, random));
    }
}
