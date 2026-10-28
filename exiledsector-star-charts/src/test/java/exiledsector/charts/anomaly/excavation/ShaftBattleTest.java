package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import exiledlib.MockSector;
import exiledsector.charts.run.ChartCampaignPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShaftBattleTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    static FleetMemberAPI ship(HullSize size) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class);
        when(spec.getHullSize()).thenReturn(size);
        when(member.getHullSpec()).thenReturn(spec);
        when(member.canBeDeployedForCombat()).thenReturn(true);
        return member;
    }

    private static CombatFleetManagerAPI manager(List<FleetMemberAPI> reserves) {
        CombatFleetManagerAPI manager = mock(CombatFleetManagerAPI.class);
        when(manager.getReservesCopy()).thenReturn(reserves);
        return manager;
    }

    @Test
    void cruisersAndCapitalsOnBothSidesHoldStationAboveTheShaft() {
        FleetMemberAPI playerFrigate = ship(HullSize.FRIGATE);
        FleetMemberAPI playerCruiser = ship(HullSize.CRUISER);
        FleetMemberAPI enemyDestroyer = ship(HullSize.DESTROYER);
        FleetMemberAPI enemyCapital = ship(HullSize.CAPITAL_SHIP);
        CombatFleetManagerAPI player = manager(List.of(playerFrigate, playerCruiser));
        CombatFleetManagerAPI enemy = manager(List.of(enemyDestroyer, enemyCapital));
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(player);
        when(engine.getFleetManager(FleetSide.ENEMY)).thenReturn(enemy);

        new ShaftBattlePlugin().init(engine);

        verify(player).removeFromReserves(playerCruiser);
        verify(player, never()).removeFromReserves(playerFrigate);
        verify(enemy).removeFromReserves(enemyCapital);
        verify(enemy, never()).removeFromReserves(enemyDestroyer);
        assertEquals(0, ShaftBattlePlugin.holdLargeHullsAbove(null));
    }

    @Test
    void thePluginChecksTheReservesAgainOnlyOnceASecond() {
        FleetMemberAPI capital = ship(HullSize.CAPITAL_SHIP);
        CombatFleetManagerAPI player = manager(List.of(capital));
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(player);
        ShaftBattlePlugin plugin = new ShaftBattlePlugin();

        plugin.init(engine);
        plugin.advance(0.5f, List.of());
        verify(player, times(1)).removeFromReserves(capital);
        plugin.advance(0.6f, List.of());
        verify(player, times(2)).removeFromReserves(capital);
    }

    @Test
    void theCavernTakesOnlyFrigatesAndDestroyersFromEitherFleet() {
        FleetMemberAPI playerFrigate = ship(HullSize.FRIGATE);
        FleetMemberAPI playerCapital = ship(HullSize.CAPITAL_SHIP);
        FleetMemberAPI enemyDestroyer = ship(HullSize.DESTROYER);
        FleetMemberAPI enemyCruiser = ship(HullSize.CRUISER);
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class, RETURNS_DEEP_STUBS);
        CampaignFleetAPI enemyFleet = mock(CampaignFleetAPI.class, RETURNS_DEEP_STUBS);
        when(playerFleet.getFleetData().getCombatReadyMembersListCopy()).thenReturn(List.of(playerFrigate, playerCapital));
        when(enemyFleet.getFleetData().getCombatReadyMembersListCopy()).thenReturn(List.of(enemyDestroyer, enemyCruiser));
        BattleCreationContext context = new BattleCreationContext(playerFleet, FleetGoal.ATTACK, enemyFleet, FleetGoal.ATTACK);
        MissionDefinitionAPI loader = mock(MissionDefinitionAPI.class);

        new ShaftBattleCreationPlugin().initBattle(context, loader);

        verify(loader).addFleetMember(FleetSide.PLAYER, playerFrigate);
        verify(loader, never()).addFleetMember(FleetSide.PLAYER, playerCapital);
        verify(loader).addFleetMember(FleetSide.ENEMY, enemyDestroyer);
        verify(loader, never()).addFleetMember(FleetSide.ENEMY, enemyCruiser);
        verify(loader).initMap(-ShaftBattleCreationPlugin.MAP_WIDTH / 2f, ShaftBattleCreationPlugin.MAP_WIDTH / 2f,
                -ShaftBattleCreationPlugin.MAP_HEIGHT / 2f, ShaftBattleCreationPlugin.MAP_HEIGHT / 2f);
        verify(loader, times(4)).addAsteroidField(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyInt());
        verify(loader).addPlugin(any(ShaftBattlePlugin.class));
        verify(loader, never()).addObjective(anyFloat(), anyFloat(), any());
        verify(loader).setHyperspaceMode(eq(false));
    }

    @Test
    void theCampaignPluginPicksTheCavernMapOnlyForAShaftBattle() {
        CampaignFleetAPI shaftFleet = mock(CampaignFleetAPI.class);
        when(shaftFleet.hasTag(ShaftFleets.FLEET_TAG)).thenReturn(true);
        CampaignFleetAPI combined = mock(CampaignFleetAPI.class);
        CampaignFleetAPI other = mock(CampaignFleetAPI.class);

        PluginPick<BattleCreationPlugin> pick = new ChartCampaignPlugin().pickBattleCreationPlugin(shaftFleet);
        assertInstanceOf(ShaftBattleCreationPlugin.class, pick.plugin);
        assertEquals(CampaignPlugin.PickPriority.MOD_SPECIFIC, pick.priority);
        assertNull(new ChartCampaignPlugin().pickBattleCreationPlugin(other));

        ShaftEncounters.expect(combined);
        assertTrue(ShaftEncounters.isShaftBattle(combined));
        assertTrue(ShaftEncounters.isShaftBattle(combined));
        assertFalse(ShaftEncounters.isShaftBattle(other));
        assertFalse(ShaftEncounters.isShaftBattle(null));
        ShaftEncounters.config(dialog -> {
        }).delegate.notifyLeave(null);
        assertFalse(ShaftEncounters.isShaftBattle(combined));
    }

    @Test
    void theEncounterKeepsOtherFleetsOutAndNeverOffersPursuitAutoresolve() {
        var config = ShaftEncounters.config(dialog -> {
        });
        assertFalse(config.pullInAllies);
        assertFalse(config.pullInEnemies);
        assertFalse(config.pullInStations);
        assertFalse(config.impactsEnemyReputation);
        assertFalse(config.dismissOnLeave);
        assertTrue(config.straightToEngage);

        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        CampaignFleetAPI enemyFleet = mock(CampaignFleetAPI.class);
        BattleCreationContext context = new BattleCreationContext(playerFleet, FleetGoal.ATTACK, enemyFleet, FleetGoal.ATTACK);
        config.delegate.battleContextCreated(null, context);
        assertFalse(context.aiRetreatAllowed);
        assertFalse(context.objectivesAllowed);
        assertTrue(ShaftEncounters.isShaftBattle(enemyFleet));
    }
}
