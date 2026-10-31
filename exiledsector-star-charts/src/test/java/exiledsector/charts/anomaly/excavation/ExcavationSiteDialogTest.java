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
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartCampaignPlugin;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcavationSiteDialogTest {

    private static final String RUN_ID = "run1";

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
        run.recordContent(new ChartContent(4, "pirates", ChartObjective.CLEAR, List.of(), null, 3, 2, List.of(ExcavationSites.ANOMALY_ID)));
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
    void theCampaignPluginOpensTheSiteOnlyWhileItStands() {
        ExcavationSite site = new ExcavationSite(0, "rock", ExcavationSite.Access.OPEN, DropType.WEAPON, 1L, false);
        ExcavationStates.record(run, site);
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
        FleetMemberAPI cruiser = ShaftBattleTest.ship(HullSize.CRUISER);
        FleetMemberAPI destroyer = ShaftBattleTest.ship(HullSize.DESTROYER);
        FleetMemberAPI mothballedFrigate = ShaftBattleTest.ship(HullSize.FRIGATE);
        when(mothballedFrigate.canBeDeployedForCombat()).thenReturn(false);

        FleetMemberAPI storedFrigate = ShaftBattleTest.ship(HullSize.FRIGATE);
        when(storedFrigate.isMothballed()).thenReturn(true);
        FleetMemberAPI civilianDestroyer = ShaftBattleTest.ship(HullSize.DESTROYER);
        when(civilianDestroyer.isCivilian()).thenReturn(true);

        assertTrue(ExcavationSiteDialog.hasShaftShips(List.of(cruiser, destroyer)));
        assertFalse(ExcavationSiteDialog.hasShaftShips(List.of(cruiser, mothballedFrigate)));
        assertFalse(ExcavationSiteDialog.hasShaftShips(List.of(storedFrigate, civilianDestroyer)));
        assertFalse(ExcavationSiteDialog.hasShaftShips(List.of()));
        assertFalse(ExcavationSiteDialog.hasShaftShips((CampaignFleetAPI) null));
    }

    @Test
    void pressingOnIsGreyedOutOnceNoFrigateOrDestroyerCanFight() {
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        FleetMemberAPI civilianDestroyer = ShaftBattleTest.ship(HullSize.DESTROYER);
        when(civilianDestroyer.isCivilian()).thenReturn(true);
        OptionPanelAPI options = mock(OptionPanelAPI.class);
        when(options.hasOption(OptionId.ENGAGE)).thenReturn(true);

        ShaftEncounterDialog.disableEngageWithoutShaftShips(options, fleetOf(ShaftBattleTest.ship(HullSize.FRIGATE)));
        verify(options, never()).setEnabled(OptionId.ENGAGE, false);

        ShaftEncounterDialog.disableEngageWithoutShaftShips(options, fleetOf(civilianDestroyer, ShaftBattleTest.ship(HullSize.CAPITAL_SHIP)));
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
    void theSiteFleetLosesAnyLargeHullAndKeepsItsLean() {
        FleetMemberAPI frigate = ShaftBattleTest.ship(HullSize.FRIGATE);
        FleetMemberAPI capital = ShaftBattleTest.ship(HullSize.CAPITAL_SHIP);
        List<FleetMemberAPI> members = new ArrayList<>(List.of(frigate, capital));
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(members));
        doAnswer(invocation -> members.remove((FleetMemberAPI) invocation.getArgument(0))).when(fleetData).removeFleetMember(capital);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleet.isEmpty()).thenAnswer(invocation -> members.isEmpty());

        assertFalse(ShaftFleets.dropLargeHulls(fleet));
        assertEquals(List.of(frigate), members);

        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getString(ShaftFleets.LEAN_KEY)).thenReturn(DropType.AI_CORE.name());
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        assertNull(ShaftFleets.leanOf(fleet));
        when(fleet.hasTag(ShaftFleets.FLEET_TAG)).thenReturn(true);
        assertEquals(DropType.AI_CORE, ShaftFleets.leanOf(fleet));
        assertTrue(ShaftFleets.isShaftFleet(fleet));
    }
}
