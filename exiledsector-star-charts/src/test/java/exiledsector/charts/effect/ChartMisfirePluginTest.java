package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartMisfirePluginTest {

    private static final float MISFIRES_PER_SECOND = 1000f;
    private static final float FRAME = 0.1f;
    private static final float SHIP_FACING = 90f;

    private CombatEngineAPI engine;
    private ShipAPI playerShip;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.isInCampaign()).thenReturn(true);
        playerShip = ship(ChartBattlePlugin.PLAYER_OWNER);
    }

    @AfterEach
    void tearDown() {
        ChartFleetConditions.clear(null);
    }

    @Test
    void aHardpointFiresWhereItPointsOnceTheFirstMinuteIsOver() {
        WeaponAPI hardpoint = weapon(WeaponType.BALLISTIC, true, 10f);
        armShip(playerShip, hardpoint);
        ChartFleetConditions.setWeaponMisfireRate(MISFIRES_PER_SECOND);
        ChartMisfirePlugin plugin = initialised();

        when(engine.getTotalElapsedTime(false)).thenReturn(ChartMisfirePlugin.GRACE_SECONDS - 1f);
        plugin.advance(FRAME, List.of());
        verify(hardpoint, never()).setForceFireOneFrame(anyBoolean());

        when(engine.getTotalElapsedTime(false)).thenReturn((float) ChartMisfirePlugin.GRACE_SECONDS);
        plugin.advance(FRAME, List.of());
        verify(hardpoint).setForceFireOneFrame(true);
        verify(hardpoint, never()).setCurrAngle(anyFloat());
        verify(hardpoint, never()).setSuspendAutomaticTurning(anyBoolean());
    }

    @Test
    void aTurretSwingsToARandomAngleInItsArcBeforeFiring() {
        float arc = 60f;
        WeaponAPI turret = weapon(WeaponType.ENERGY, false, arc);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getTurnRate()).thenReturn(1f);
        when(turret.getSpec()).thenReturn(spec);
        when(turret.getCurrAngle()).thenReturn(SHIP_FACING);
        armShip(playerShip, turret);
        ChartFleetConditions.setWeaponMisfireRate(MISFIRES_PER_SECOND);
        ChartMisfirePlugin plugin = initialised();
        when(engine.getTotalElapsedTime(false)).thenReturn((float) ChartMisfirePlugin.GRACE_SECONDS);

        plugin.advance(FRAME, List.of());
        verify(turret).setSuspendAutomaticTurning(true);
        verify(turret, never()).setForceFireOneFrame(anyBoolean());

        when(spec.getTurnRate()).thenReturn(10000f);
        plugin.advance(FRAME, List.of());
        verify(turret).setForceFireOneFrame(true);
        verify(turret, atLeastOnce()).setCurrAngle(floatThat(angle -> Math.abs(angle - SHIP_FACING) <= arc / 2f));

        when(engine.isCombatOver()).thenReturn(true);
        plugin.advance(FRAME, List.of());
        verify(turret).setSuspendAutomaticTurning(false);
    }

    @Test
    void nothingMisfiresOutsideAnErraticChartOrInSimulations() {
        WeaponAPI hardpoint = weapon(WeaponType.BALLISTIC, true, 10f);
        armShip(playerShip, hardpoint);
        when(engine.getTotalElapsedTime(false)).thenReturn(ChartMisfirePlugin.GRACE_SECONDS * 2f);
        initialised().advance(FRAME, List.of());

        ChartFleetConditions.setWeaponMisfireRate(MISFIRES_PER_SECOND);
        when(engine.isSimulation()).thenReturn(true);
        initialised().advance(FRAME, List.of());

        verify(hardpoint, never()).setForceFireOneFrame(anyBoolean());
    }

    @Test
    void onlyNonMissileWeaponsOnLivePlayerSideShipsMisfire() {
        WeaponAPI gun = weapon(WeaponType.BALLISTIC, true, 10f);
        WeaponAPI missile = weapon(WeaponType.MISSILE, true, 10f);
        WeaponAPI decorative = weapon(WeaponType.ENERGY, true, 10f);
        when(decorative.isDecorative()).thenReturn(true);
        armShip(playerShip, gun, missile, decorative);
        ShipAPI enemy = ship(ChartBattlePlugin.ENEMY_OWNER);
        armShip(enemy, weapon(WeaponType.ENERGY, true, 10f));
        ShipAPI fighter = ship(ChartBattlePlugin.PLAYER_OWNER);
        when(fighter.isFighter()).thenReturn(true);
        armShip(fighter, weapon(WeaponType.ENERGY, true, 10f));
        ShipAPI module = ship(ChartBattlePlugin.PLAYER_OWNER);
        when(module.getParentStation()).thenReturn(playerShip);
        WeaponAPI moduleGun = weapon(WeaponType.ENERGY, false, 360f);
        armShip(module, moduleGun);

        List<ChartMisfirePlugin.Armed> armed = ChartMisfirePlugin.armedWeapons(List.of(playerShip, enemy, fighter, module));

        assertEquals(List.of(new ChartMisfirePlugin.Armed(playerShip, gun), new ChartMisfirePlugin.Armed(module, moduleGun)), armed);
        assertFalse(ChartMisfirePlugin.canMisfire(missile));
        assertTrue(ChartMisfirePlugin.canMisfire(gun));
    }

    @Test
    void aWeaponStillCoolingDownHoldsItsFireUntilItCanShootAgain() {
        WeaponAPI hardpoint = weapon(WeaponType.BALLISTIC, true, 10f);
        when(hardpoint.isFiring()).thenReturn(true);
        when(hardpoint.getCooldownRemaining()).thenReturn(2f);
        armShip(playerShip, hardpoint);
        ChartFleetConditions.setWeaponMisfireRate(MISFIRES_PER_SECOND);
        ChartMisfirePlugin plugin = initialised();
        when(engine.getTotalElapsedTime(false)).thenReturn((float) ChartMisfirePlugin.GRACE_SECONDS);

        plugin.advance(FRAME, List.of());
        verify(hardpoint).setForceNoFireOneFrame(true);
        verify(hardpoint, never()).setForceFireOneFrame(anyBoolean());

        when(hardpoint.isFiring()).thenReturn(false);
        when(hardpoint.getCooldownRemaining()).thenReturn(0f);
        plugin.advance(FRAME, List.of());
        verify(hardpoint).setForceFireOneFrame(true);
    }

    @Test
    void aTurretSwingsThroughItsArcRatherThanItsBlindSpot() {
        WeaponAPI turret = weapon(WeaponType.ENERGY, false, 270f);
        when(turret.getCurrAngle()).thenReturn(SHIP_FACING + 130f);

        assertFalse(ChartMisfirePlugin.swing(turret, SHIP_FACING, -130f, 10f));
        verify(turret).setCurrAngle(SHIP_FACING + 120f);
    }

    @Test
    void turnsTakeTheShortWayRound() {
        assertEquals(20f, ChartMisfirePlugin.shortestTurn(350f, 10f), 0.001f);
        assertEquals(-20f, ChartMisfirePlugin.shortestTurn(10f, 350f), 0.001f);
        assertEquals(-90f, ChartMisfirePlugin.shortestTurn(90f, 0f), 0.001f);
    }

    private ChartMisfirePlugin initialised() {
        ChartMisfirePlugin plugin = new ChartMisfirePlugin(new Random(1L));
        plugin.init(engine);
        return plugin;
    }

    private void armShip(ShipAPI ship, WeaponAPI... weapons) {
        when(ship.getAllWeapons()).thenReturn(List.of(weapons));
        List<ShipAPI> ships = new ArrayList<>(engine.getShips() == null ? List.of() : engine.getShips());
        ships.add(ship);
        when(engine.getShips()).thenReturn(ships);
    }

    private static WeaponAPI weapon(WeaponType type, boolean hardpoint, float arc) {
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.isHardpoint()).thenReturn(hardpoint);
        when(slot.isTurret()).thenReturn(!hardpoint);
        when(slot.computeMidArcAngle(any())).thenReturn(SHIP_FACING);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        when(weapon.getSlot()).thenReturn(slot);
        when(weapon.getArc()).thenReturn(arc);
        return weapon;
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }
}
