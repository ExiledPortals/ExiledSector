package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArcStrikeSpeedListenerTest {

    private static ShipAPI ship(float speedPercent) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship");
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.getMutableStats().getDynamic().getValue(ArcStrikeSpeedListener.SPEED_PERCENT_KEY, 0f)).thenReturn(speedPercent);
        return ship;
    }

    private static String strike(ArcStrikeSpeedListener listener, ShipAPI ship, Object param) {
        return listener.modifyDamageTaken(param, ship, mock(DamageAPI.class), new Vector2f(), false);
    }

    @Test
    void anEmpArcStrikeRaisesTopSpeedForTheBurst() {
        ShipAPI ship = ship(20f);
        ArcStrikeSpeedListener listener = new ArcStrikeSpeedListener(ship);

        assertNull(strike(listener, ship, mock(EmpArcEntityAPI.class)));

        assertTrue(listener.bursting());
        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent(anyString(), eq(20f));
        verify(ship.getMutableStats().getMaxTurnRate()).modifyPercent(anyString(), eq(20f));
    }

    @Test
    void otherHitsStartNoBurst() {
        ShipAPI ship = ship(20f);
        ArcStrikeSpeedListener listener = new ArcStrikeSpeedListener(ship);

        strike(listener, ship, mock(DamagingProjectileAPI.class));

        assertFalse(listener.bursting());
        verify(ship.getMutableStats().getMaxSpeed(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void theBurstEndsAfterItsSecondsAndANewStrikeRestartsIt() {
        ShipAPI ship = ship(20f);
        MutableStat topSpeed = ship.getMutableStats().getMaxSpeed();
        ArcStrikeSpeedListener listener = new ArcStrikeSpeedListener(ship);
        strike(listener, ship, mock(EmpArcEntityAPI.class));

        listener.advance(ArcStrikeSpeedListener.BURST_SECONDS - 1f);
        strike(listener, ship, mock(EmpArcEntityAPI.class));
        listener.advance(ArcStrikeSpeedListener.BURST_SECONDS - 1f);
        assertTrue(listener.bursting());
        verify(topSpeed, never()).unmodify(anyString());

        listener.advance(1f);
        assertFalse(listener.bursting());
        verify(topSpeed).unmodify(anyString());
    }
}
