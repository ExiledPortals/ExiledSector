package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullLossLungeListenerTest {

    private static ShipAPI ship(float speedPercent) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship");
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.getHullLevel()).thenReturn(1f);
        when(ship.getMutableStats().getDynamic().getValue(HullLossLungeListener.SPEED_PERCENT_KEY, 0f)).thenReturn(speedPercent);
        return ship;
    }

    private static void loseHullTo(ShipAPI ship, HullLossLungeListener listener, float hullLevel) {
        when(ship.getHullLevel()).thenReturn(hullLevel);
        listener.advance(0.1f);
    }

    @Test
    void losingAShareOfMaximumHullStartsTheBurst() {
        ShipAPI ship = ship(25f);
        HullLossLungeListener listener = new HullLossLungeListener(ship, lungingShip -> false);
        listener.advance(0.1f);

        loseHullTo(ship, listener, 0.95f);
        assertFalse(listener.bursting());
        loseHullTo(ship, listener, 0.9f);

        assertTrue(listener.bursting());
        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent(anyString(), eq(25f));
    }

    @Test
    void theBurstEndsAndTheNextWaitsForTheCooldown() {
        ShipAPI ship = ship(25f);
        MutableStat topSpeed = ship.getMutableStats().getMaxSpeed();
        HullLossLungeListener listener = new HullLossLungeListener(ship, lungingShip -> false);
        listener.advance(0.1f);
        loseHullTo(ship, listener, 0.9f);

        listener.advance(HullLossLungeListener.BURST_SECONDS);
        assertFalse(listener.bursting());
        verify(topSpeed).unmodify(anyString());

        loseHullTo(ship, listener, 0.7f);
        assertFalse(listener.bursting());

        listener.advance(HullLossLungeListener.COOLDOWN_SECONDS);
        loseHullTo(ship, listener, 0.6f);
        assertTrue(listener.bursting());
    }

    @Test
    void aShipDeployedDamagedMeasuresLossFromItsDeploymentHull() {
        ShipAPI ship = ship(25f);
        when(ship.getHullLevel()).thenReturn(0.5f);
        HullLossLungeListener listener = new HullLossLungeListener(ship, lungingShip -> false);

        listener.advance(0.1f);

        assertFalse(listener.bursting());
    }

    @Test
    void theBurstWaitsWhileTheMantleLunges() {
        ShipAPI ship = ship(25f);
        AtomicBoolean mantleLunging = new AtomicBoolean(true);
        HullLossLungeListener listener = new HullLossLungeListener(ship, lungingShip -> mantleLunging.get());
        listener.advance(0.1f);

        loseHullTo(ship, listener, 0.85f);
        assertFalse(listener.bursting());

        mantleLunging.set(false);
        listener.advance(0.1f);
        assertTrue(listener.bursting());
    }
}
