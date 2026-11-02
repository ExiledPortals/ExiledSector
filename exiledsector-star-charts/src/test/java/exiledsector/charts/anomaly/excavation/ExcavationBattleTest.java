package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import exiledsector.MockSector;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.run.ChartCampaignPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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

class ExcavationBattleTest {

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

    private static CombatEngineAPI engineAgainst(CampaignFleetAPI opponent) {
        BattleCreationContext context = new BattleCreationContext(mock(CampaignFleetAPI.class), FleetGoal.ATTACK, opponent, FleetGoal.ATTACK);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getContext()).thenReturn(context);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        return engine;
    }

    private static CampaignFleetAPI excavationFleet() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.hasTag(ExcavationFleets.FLEET_TAG)).thenReturn(true);
        return fleet;
    }

    @Test
    void cruisersAndCapitalsOnBothSidesHoldStationAboveTheShaft() {
        FleetMemberAPI playerFrigate = ship(HullSize.FRIGATE);
        FleetMemberAPI playerCruiser = ship(HullSize.CRUISER);
        FleetMemberAPI enemyDestroyer = ship(HullSize.DESTROYER);
        FleetMemberAPI enemyCapital = ship(HullSize.CAPITAL_SHIP);
        CombatFleetManagerAPI player = manager(List.of(playerFrigate, playerCruiser));
        CombatFleetManagerAPI enemy = manager(List.of(enemyDestroyer, enemyCapital));
        CombatEngineAPI engine = engineAgainst(excavationFleet());
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(player);
        when(engine.getFleetManager(FleetSide.ENEMY)).thenReturn(enemy);

        new ExcavationBattlePlugin().init(engine);

        verify(player).removeFromReserves(playerCruiser);
        verify(player, never()).removeFromReserves(playerFrigate);
        verify(enemy).removeFromReserves(enemyCapital);
        verify(enemy, never()).removeFromReserves(enemyDestroyer);
        assertEquals(0, ExcavationBattlePlugin.holdLargeHullsAbove(null));
    }

    @Test
    void largeHullsHoldBackEvenWhenAnotherBattlePluginBuiltTheMap() {
        FleetMemberAPI capital = ship(HullSize.CAPITAL_SHIP);
        CombatFleetManagerAPI player = manager(List.of(capital));
        CampaignFleetAPI combined = mock(CampaignFleetAPI.class);
        MemoryAPI combinedMemory = FakeChartContentEngine.memory();
        combinedMemory.set(ExcavationEncounters.OPPONENT_KEY, true);
        when(combined.getMemoryWithoutUpdate()).thenReturn(combinedMemory);
        CombatEngineAPI engine = engineAgainst(combined);
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(player);

        assertFalse(ExcavationBattlePlugin.onCavernMap(engine));
        new ExcavationBattlePlugin().init(engine);

        verify(player).removeFromReserves(capital);
    }

    @Test
    void otherBattlesKeepTheirLargeHulls() {
        FleetMemberAPI capital = ship(HullSize.CAPITAL_SHIP);
        CombatFleetManagerAPI player = manager(List.of(capital));
        CombatEngineAPI engine = engineAgainst(mock(CampaignFleetAPI.class));
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(player);
        CombatEngineAPI simulator = mock(CombatEngineAPI.class);
        when(simulator.getFleetManager(FleetSide.PLAYER)).thenReturn(player);
        ExcavationBattlePlugin plugin = new ExcavationBattlePlugin();

        plugin.init(engine);
        plugin.advance(2f, List.of());
        plugin.init(simulator);
        plugin.advance(2f, List.of());

        verify(player, never()).removeFromReserves(any());
    }

    @Test
    void thePluginChecksTheReservesAgainOnlyOnceASecond() {
        FleetMemberAPI capital = ship(HullSize.CAPITAL_SHIP);
        CombatFleetManagerAPI player = manager(List.of(capital));
        CombatEngineAPI engine = engineAgainst(excavationFleet());
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(player);
        ExcavationBattlePlugin plugin = new ExcavationBattlePlugin();

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

        new ExcavationBattleCreationPlugin().initBattle(context, loader);

        verify(loader).addFleetMember(FleetSide.PLAYER, playerFrigate);
        verify(loader, never()).addFleetMember(FleetSide.PLAYER, playerCapital);
        verify(loader).addFleetMember(FleetSide.ENEMY, enemyDestroyer);
        verify(loader, never()).addFleetMember(FleetSide.ENEMY, enemyCruiser);
        verify(loader).initMap(-ExcavationBattleCreationPlugin.MAP_WIDTH / 2f, ExcavationBattleCreationPlugin.MAP_WIDTH / 2f,
                -ExcavationBattleCreationPlugin.MAP_HEIGHT / 2f, ExcavationBattleCreationPlugin.MAP_HEIGHT / 2f);
        verify(loader, times(4)).addAsteroidField(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyInt());
        verify(loader, never()).addPlugin(any());
        verify(loader, never()).addObjective(anyFloat(), anyFloat(), any());
        verify(loader).setHyperspaceMode(eq(false));

        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        new ExcavationBattleCreationPlugin().afterDefinitionLoad(engine);
        assertTrue(ExcavationBattlePlugin.onCavernMap(engine));
    }

    @Test
    void theCampaignPluginPicksTheCavernMapOnlyForAShaftBattle() {
        CampaignFleetAPI excavationFleet = mock(CampaignFleetAPI.class);
        when(excavationFleet.hasTag(ExcavationFleets.FLEET_TAG)).thenReturn(true);
        CampaignFleetAPI combined = mock(CampaignFleetAPI.class);
        MemoryAPI combinedMemory = FakeChartContentEngine.memory();
        when(combined.getMemoryWithoutUpdate()).thenReturn(combinedMemory);
        CampaignFleetAPI other = mock(CampaignFleetAPI.class);

        PluginPick<BattleCreationPlugin> pick = new ChartCampaignPlugin().pickBattleCreationPlugin(excavationFleet);
        assertInstanceOf(ExcavationBattleCreationPlugin.class, pick.plugin);
        assertEquals(CampaignPlugin.PickPriority.MOD_SPECIFIC, pick.priority);
        assertNull(new ChartCampaignPlugin().pickBattleCreationPlugin(other));

        assertFalse(ExcavationEncounters.isExcavationBattle(combined));
        ExcavationEncounters.config(dialog -> {
        }).delegate.battleContextCreated(null, new BattleCreationContext(mock(CampaignFleetAPI.class), FleetGoal.ATTACK, combined, FleetGoal.ATTACK));
        assertTrue(ExcavationEncounters.isExcavationBattle(combined));
        assertInstanceOf(ExcavationBattleCreationPlugin.class, new ChartCampaignPlugin().pickBattleCreationPlugin(combined).plugin);
        assertFalse(ExcavationEncounters.isExcavationBattle(other));
        assertFalse(ExcavationEncounters.isExcavationBattle(null));
    }

    @Test
    void theEncounterKeepsOtherFleetsOutAndNeverOffersPursuitAutoresolve() {
        var config = ExcavationEncounters.config(dialog -> {
        });
        assertFalse(config.pullInAllies);
        assertFalse(config.pullInEnemies);
        assertFalse(config.pullInStations);
        assertFalse(config.impactsEnemyReputation);
        assertFalse(config.dismissOnLeave);
        assertTrue(config.straightToEngage);

        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        CampaignFleetAPI enemyFleet = mock(CampaignFleetAPI.class);
        MemoryAPI enemyMemory = FakeChartContentEngine.memory();
        when(enemyFleet.getMemoryWithoutUpdate()).thenReturn(enemyMemory);
        BattleCreationContext context = new BattleCreationContext(playerFleet, FleetGoal.ATTACK, enemyFleet, FleetGoal.ATTACK);
        config.delegate.battleContextCreated(null, context);
        assertFalse(context.aiRetreatAllowed);
        assertFalse(context.objectivesAllowed);
        assertTrue(ExcavationEncounters.isExcavationBattle(enemyFleet));
    }
}
