package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.OptionId;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.anomaly.debris.AnomalousDebris;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.run.ChartCampaignPlugin;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.i18n.Translation;
import exiledsector.items.crafting.material.Engrams;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.ModifierSources;
import exiledsector.loot.Loot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcavationSiteDialogTest {

    private static final String RUN_ID = "run1";
    private static final String ENGRAM = "exiledSector_test_excavationEngram";

    private MockSector mockSector;
    private StarSystemAPI system;
    private ChartRun run;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        when(system.getId()).thenReturn("charted space");
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
        run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(4, "pirates", null, 3, 2, List.of(ExcavationSites.ANOMALY_ID)));
        ChartRuns.store(run);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private PlanetAPI planet(String id, StarSystemAPI location) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getId()).thenReturn(id);
        when(planet.getContainingLocation()).thenReturn(location);
        return planet;
    }

    @Test
    void eachSiteIsScoutedOnce() {
        ExcavationSite rock = new ExcavationSite(0, "rock", ExcavationSite.Access.OPEN, 1L, false);
        ExcavationSite ice = new ExcavationSite(1, "ice", ExcavationSite.Access.COLLAPSED, 2L, false);
        ExcavationStates.record(ExcavationStates.of(run), rock);
        ExcavationStates.record(ExcavationStates.of(run), ice);

        assertTrue(ExcavationStates.scout(run, rock));
        assertFalse(ExcavationStates.scout(run, rock));
        assertTrue(ExcavationStates.scout(run, ice));
        assertFalse(ExcavationStates.scout(run, ice));
        assertEquals(List.of(rock, ice), ExcavationStates.all(run));
    }

    @Test
    void everyAccessHasScoutingText() {
        for (ExcavationSite.Access access : ExcavationSite.Access.values()) {
            assertTrue(Translation.has(ExcavationSiteDialog.scoutedKey(access)), access.name());
        }
        assertTrue(Translation.has("chart.anomaly.excavation_sites.dialog.stores"));
        assertTrue(Translation.has("chart.anomaly.excavation_sites.dialog.findsItems"));
    }

    @Test
    void everyShaftSizeHasItsOwnText() {
        for (int maxShipSize = ExcavationRules.FRIGATES_ONLY_SHIP_SIZE; maxShipSize <= ExcavationRules.WIDENED_SHIP_SIZE; maxShipSize++) {
            for (String key : List.of("chart.anomaly.excavation_sites.dialog.option.descend", "chart.anomaly.excavation_sites.dialog.descendTooltip",
                    ExcavationSiteDialog.NO_SHAFT_SHIPS_KEY)) {
                assertTrue(Translation.has(ExcavationSiteDialog.sizedKey(key, maxShipSize)), key + " " + maxShipSize);
            }
        }
        assertEquals(ExcavationSiteDialog.NO_SHAFT_SHIPS_KEY, ExcavationSiteDialog.sizedKey(ExcavationSiteDialog.NO_SHAFT_SHIPS_KEY, ExcavationRules.MAX_SHIP_SIZE));
        for (String key : List.of("secondCavern", "option.deeper", "deeperTooltip", "option.surface", "corruptTooltip")) {
            assertTrue(Translation.has("chart.anomaly.excavation_sites.dialog." + key), key);
        }
        assertTrue(Translation.has(ExcavationFleets.fleetNameKey(false, ExcavationGuardians.FIRST_CAVERN.asCorrupted())));
    }

    @Test
    void cavernFleetsAreOnlyCorruptibleWithAnomalousDebrisInTheChart() {
        PlanetAPI rock = planet("rock", system);
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = memory(stored);
        when(rock.getMemoryWithoutUpdate()).thenReturn(memory);
        ExcavationTuning corruptible = ExcavationTuning.of(ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXCAVATION_CORRUPTIBLE_GUARDIANS, 1f))));
        ChartRun withDebris = LiveAnomalyRuns.run(4, ExcavationSites.ANOMALY_ID, AnomalousDebris.ANOMALY_ID);

        assertFalse(ExcavationCorruption.offered(run, corruptible, rock));
        assertFalse(ExcavationCorruption.offered(withDebris, ExcavationTuning.NONE, rock));
        assertFalse(ExcavationCorruption.offered(null, corruptible, rock));
        assertTrue(ExcavationCorruption.offered(withDebris, corruptible, rock));

        CampaignFleetAPI held = mock(CampaignFleetAPI.class);
        stored.put(ExcavationFleets.SITE_FLEET_KEY, held);
        assertFalse(ExcavationCorruption.offered(withDebris, corruptible, rock));
        when(held.isEmpty()).thenReturn(true);
        assertTrue(ExcavationCorruption.offered(withDebris, corruptible, rock));
    }

    @Test
    void aCorruptedCavernFleetDropsItsEngramOnce() throws Exception {
        ItemModifiers.register(new JSONArray().put(new JSONObject().put("id", "HULL_PERCENT_EXCAVATION_TEST").put("affix", "prefix")
                .put("scopes", "hull_item").put("lines", "HULL_PERCENT:15:15").put("name", "Revenant")));
        Engrams.register(new JSONArray().put(new JSONObject().put("id", ENGRAM).put("name", "Engram")
                .put("modifier", "HULL_PERCENT_EXCAVATION_TEST").put("item_types", "hull_item").put("min_tier", "1").put("weight", "1")));
        try {
            Map<String, Object> stored = new HashMap<>();
            MemoryAPI memory = memory(stored);
            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
            when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
            when(fleet.hasTag(ExcavationFleets.FLEET_TAG)).thenReturn(true);

            assertEquals(ExcavationGuardians.FIRST_CAVERN, ExcavationFleets.guardiansOf(fleet));
            assertEquals(ExcavationGuardians.FIRST_CAVERN, ExcavationFleets.guardiansOf(null));
            stored.put(ExcavationFleets.SECOND_CAVERN_KEY, true);
            stored.put(ExcavationFleets.CORRUPTED_KEY, true);
            assertEquals(new ExcavationGuardians(true, true), ExcavationFleets.guardiansOf(fleet));

            Loot loot = ExcavationLoot.fleetItems(fleet, ChartLootContext.atTier(4), 0, 1f, new Random(1L));
            assertEquals(1, loot.materials().getOrDefault(ENGRAM, 0));
            Loot again = ExcavationLoot.fleetItems(fleet, ChartLootContext.atTier(4), 0, 1f, new Random(1L));
            assertTrue(!again.materials().containsKey(ENGRAM));
            assertNull(ExcavationLoot.engramOnce(null, 4, new Random(1L)));
        } finally {
            Engrams.unregisterAll();
            ModifierSources.clear();
            ItemModifiers.clear();
        }
    }

    private static MemoryAPI memory(Map<String, Object> stored) {
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        when(memory.get(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)));
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        return memory;
    }

    @Test
    void theCampaignPluginOpensTheSiteOnlyWhileItStands() {
        ExcavationSite site = new ExcavationSite(0, "rock", ExcavationSite.Access.OPEN, 1L, false);
        ExcavationStates.record(ExcavationStates.of(run), site);
        PlanetAPI rock = planet("rock", system);
        PlanetAPI bare = planet("bare", system);
        PlanetAPI elsewhere = planet("rock", mock(StarSystemAPI.class));

        PluginPick<InteractionDialogPlugin> pick = new ChartCampaignPlugin().pickInteractionDialogPlugin(rock);
        assertInstanceOf(ExcavationSiteDialog.class, pick.plugin);
        assertEquals(CampaignPlugin.PickPriority.MOD_SPECIFIC, pick.priority);
        assertFalse(ExcavationSiteDialog.wantsToHandle(run, bare));
        assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(elsewhere));
        assertFalse(ExcavationSiteDialog.wantsToHandle(run, mock(CampaignFleetAPI.class)));

        ExcavationStates.clear(run, site);
        assertFalse(ExcavationSiteDialog.wantsToHandle(run, rock));
        assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(rock));
    }

    @Test
    void goingDownTheShaftNeedsAFrigateOrDestroyerThatCanDeploy() {
        FleetMemberAPI cruiser = ExcavationBattleTest.ship(HullSize.CRUISER);
        FleetMemberAPI destroyer = ExcavationBattleTest.ship(HullSize.DESTROYER);
        FleetMemberAPI mothballedFrigate = ExcavationBattleTest.ship(HullSize.FRIGATE);
        when(mothballedFrigate.canBeDeployedForCombat()).thenReturn(false);

        FleetMemberAPI storedFrigate = ExcavationBattleTest.ship(HullSize.FRIGATE);
        when(storedFrigate.isMothballed()).thenReturn(true);
        FleetMemberAPI civilianDestroyer = ExcavationBattleTest.ship(HullSize.DESTROYER);
        when(civilianDestroyer.isCivilian()).thenReturn(true);

        assertTrue(ExcavationSiteDialog.hasExcavationShips(List.of(cruiser, destroyer), ExcavationRules.MAX_SHIP_SIZE));
        assertFalse(ExcavationSiteDialog.hasExcavationShips(List.of(cruiser, mothballedFrigate), ExcavationRules.MAX_SHIP_SIZE));
        assertFalse(ExcavationSiteDialog.hasExcavationShips(List.of(storedFrigate, civilianDestroyer), ExcavationRules.MAX_SHIP_SIZE));
        assertFalse(ExcavationSiteDialog.hasExcavationShips(List.of(), ExcavationRules.MAX_SHIP_SIZE));
        assertFalse(ExcavationSiteDialog.hasExcavationShips((CampaignFleetAPI) null, ExcavationRules.MAX_SHIP_SIZE));
    }

    @Test
    void pressingOnIsGreyedOutOnceNoFrigateOrDestroyerCanFight() {
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        FleetMemberAPI civilianDestroyer = ExcavationBattleTest.ship(HullSize.DESTROYER);
        when(civilianDestroyer.isCivilian()).thenReturn(true);
        OptionPanelAPI options = mock(OptionPanelAPI.class);
        when(options.hasOption(OptionId.ENGAGE)).thenReturn(true);

        ExcavationEncounterDialog.disableEngageWithoutExcavationShips(options, fleetOf(ExcavationBattleTest.ship(HullSize.FRIGATE)), ExcavationRules.MAX_SHIP_SIZE);
        verify(options, never()).setEnabled(OptionId.ENGAGE, false);

        ExcavationEncounterDialog.disableEngageWithoutExcavationShips(options, fleetOf(civilianDestroyer, ExcavationBattleTest.ship(HullSize.CAPITAL_SHIP)), ExcavationRules.MAX_SHIP_SIZE);
        verify(options).setEnabled(OptionId.ENGAGE, false);
    }

    private static CampaignFleetAPI fleetOf(FleetMemberAPI... members) {
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getCombatReadyMembersListCopy()).thenReturn(new ArrayList<>(List.of(members)));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFleetData()).thenReturn(fleetData);
        return fleet;
    }

    @Test
    void theSiteFleetLosesAnyLargeHull() {
        FleetMemberAPI frigate = ExcavationBattleTest.ship(HullSize.FRIGATE);
        FleetMemberAPI capital = ExcavationBattleTest.ship(HullSize.CAPITAL_SHIP);
        List<FleetMemberAPI> members = new ArrayList<>(List.of(frigate, capital));
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(members));
        doAnswer(invocation -> members.remove((FleetMemberAPI) invocation.getArgument(0))).when(fleetData).removeFleetMember(capital);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleet.isEmpty()).thenAnswer(invocation -> members.isEmpty());

        assertFalse(ExcavationFleets.dropLargeHulls(fleet, ExcavationRules.MAX_SHIP_SIZE));
        assertEquals(List.of(frigate), members);

        assertFalse(ExcavationFleets.isExcavationFleet(fleet));
        when(fleet.hasTag(ExcavationFleets.FLEET_TAG)).thenReturn(true);
        assertTrue(ExcavationFleets.isExcavationFleet(fleet));
    }
}
