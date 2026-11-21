package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlliedShipTargetsTest {

    private CombatEngineAPI engine;
    private final List<ShipAPI> ships = new ArrayList<>();

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        when(engine.getShips()).thenReturn(ships);
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getParentStation()).thenReturn(null);
        return ship;
    }

    private void at(float seconds) {
        when(engine.getTotalElapsedTime(false)).thenReturn(seconds);
    }

    @Test
    void everyListenerOnASideReadsOneSnapshotScannedAtMostEveryHalfSecond() {
        ships.addAll(List.of(ship(0), ship(0), ship(1)));

        AlliedShipTargets first = AlliedShipTargets.forSide(engine, 0);
        AlliedShipTargets second = AlliedShipTargets.forSide(engine, 0);
        at(AlliedShipTargets.REFRESH_INTERVAL_SECONDS * 0.9f);
        AlliedShipTargets.forSide(engine, 0);

        assertSame(first, second);
        verify(engine, times(1)).getShips();
    }

    @Test
    void aNewAllyTargetIsSeenOnceTheIntervalHasPassed() {
        ShipAPI owner = ship(0);
        ShipAPI ally = ship(0);
        ShipAPI enemy = ship(1);
        ships.addAll(List.of(owner, ally, enemy));
        AlliedShipTargets targets = AlliedShipTargets.forSide(engine, 0);

        when(ally.getShipTarget()).thenReturn(enemy);
        at(0.1f);
        AlliedShipTargets.forSide(engine, 0);
        assertFalse(targets.isTargetedByAllyOf(owner, enemy));

        at(AlliedShipTargets.REFRESH_INTERVAL_SECONDS);
        AlliedShipTargets.forSide(engine, 0);
        assertTrue(targets.isTargetedByAllyOf(owner, enemy));
    }

    @Test
    void eachSideKeepsItsOwnTargets() {
        ShipAPI playerShip = ship(0);
        ShipAPI enemyShip = ship(1);
        ShipAPI enemyWingman = ship(1);
        when(enemyWingman.getShipTarget()).thenReturn(playerShip);
        ships.addAll(List.of(playerShip, enemyShip, enemyWingman));

        AlliedShipTargets playerSide = AlliedShipTargets.forSide(engine, 0);
        AlliedShipTargets enemySide = AlliedShipTargets.forSide(engine, 1);

        assertNotSame(playerSide, enemySide);
        assertTrue(enemySide.isTargetedByAllyOf(enemyShip, playerShip));
        assertFalse(playerSide.isTargetedByAllyOf(playerShip, playerShip));
    }

    @Test
    void deadOrNeutralTargetsAndDeadTargetersAreLeftOut() {
        ShipAPI owner = ship(0);
        ShipAPI hulk = ship(1);
        when(hulk.isHulk()).thenReturn(true);
        ShipAPI neutral = ship(100);
        ShipAPI enemy = ship(1);
        ShipAPI deadAlly = ship(0);
        when(deadAlly.isAlive()).thenReturn(false);
        when(deadAlly.getShipTarget()).thenReturn(enemy);
        ShipAPI ally = ship(0);
        when(ally.getShipTarget()).thenReturn(hulk);
        ShipAPI otherAlly = ship(0);
        when(otherAlly.getShipTarget()).thenReturn(neutral);
        ships.addAll(List.of(owner, hulk, neutral, enemy, deadAlly, ally, otherAlly));

        AlliedShipTargets targets = AlliedShipTargets.forSide(engine, 0);

        assertFalse(targets.isTargetedByAllyOf(owner, hulk));
        assertFalse(targets.isTargetedByAllyOf(owner, neutral));
        assertFalse(targets.isTargetedByAllyOf(owner, enemy));
    }
}
