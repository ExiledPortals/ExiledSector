package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartArcStrikePluginTest {

    private static final float EMP = 800f;

    private CombatEngineAPI engine;
    private ShipAPI playerShip;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.isInCampaign()).thenReturn(true);
        playerShip = ship(ChartBattlePlugin.PLAYER_OWNER);
        ShipAPI enemyShip = ship(ChartBattlePlugin.ENEMY_OWNER);
        when(engine.getShips()).thenReturn(List.of(enemyShip, playerShip));
    }

    @AfterEach
    void tearDown() {
        ChartFleetConditions.clear(null);
    }

    @Test
    void everyIntervalAShieldPiercingEmpOnlyArcStrikesAPlayerSideShip() {
        ChartFleetConditions.setArcStrikeEmp(EMP);
        ChartArcStrikePlugin plugin = initialised();

        plugin.advance(ChartArcStrikePlugin.INTERVAL_SECONDS - 1f, List.of());
        verifyStrikes(0);
        plugin.advance(1f, List.of());
        plugin.advance(ChartArcStrikePlugin.INTERVAL_SECONDS, List.of());
        verifyStrikes(2);
        verify(engine, never()).spawnEmpArc(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(), anyString(), anyFloat(), any(), any());
    }

    @Test
    void noArcsStrikeOutsideAnIonizedChartOrInSimulations() {
        ChartArcStrikePlugin outside = initialised();
        outside.advance(ChartArcStrikePlugin.INTERVAL_SECONDS * 2f, List.of());

        ChartFleetConditions.setArcStrikeEmp(EMP);
        when(engine.isSimulation()).thenReturn(true);
        ChartArcStrikePlugin simulation = initialised();
        simulation.advance(ChartArcStrikePlugin.INTERVAL_SECONDS * 2f, List.of());

        verifyStrikes(0);
    }

    @Test
    void onlyLivePlayerSideShipsThatCanBeHitAreTargets() {
        ShipAPI fighter = ship(ChartBattlePlugin.PLAYER_OWNER);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI phased = ship(ChartBattlePlugin.PLAYER_OWNER);
        when(phased.isPhased()).thenReturn(true);
        ShipAPI wreck = ship(ChartBattlePlugin.PLAYER_OWNER);
        when(wreck.isHulk()).thenReturn(true);
        ShipAPI module = ship(ChartBattlePlugin.PLAYER_OWNER);
        when(module.getParentStation()).thenReturn(playerShip);

        assertTrue(ChartArcStrikePlugin.canBeStruck(playerShip));
        assertFalse(ChartArcStrikePlugin.canBeStruck(ship(ChartBattlePlugin.ENEMY_OWNER)));
        assertFalse(ChartArcStrikePlugin.canBeStruck(fighter));
        assertFalse(ChartArcStrikePlugin.canBeStruck(phased));
        assertFalse(ChartArcStrikePlugin.canBeStruck(wreck));
        assertFalse(ChartArcStrikePlugin.canBeStruck(module));
        assertSame(playerShip, ChartArcStrikePlugin.pickTarget(List.of(fighter, phased, playerShip, wreck), new Random(1L)));
    }

    private void verifyStrikes(int strikes) {
        verify(engine, times(strikes)).spawnEmpArcPierceShields(isNull(), any(Vector2f.class), isNull(), eq((CombatEntityAPI) playerShip),
                eq(DamageType.ENERGY), eq(0f), eq(EMP), anyFloat(), anyString(), anyFloat(), any(Color.class), any(Color.class));
    }

    private ChartArcStrikePlugin initialised() {
        ChartArcStrikePlugin plugin = new ChartArcStrikePlugin(new Random(1L));
        plugin.init(engine);
        return plugin;
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }
}
