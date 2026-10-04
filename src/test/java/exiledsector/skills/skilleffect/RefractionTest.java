package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefractionTest {

    private static ShipAPI shieldedShipAtOrigin(float shieldRadius) {
        ShipAPI ship = mock(ShipAPI.class);
        ShieldAPI shield = mock(ShieldAPI.class);
        when(ship.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(0f, 0f));
        when(ship.getShield()).thenReturn(shield);
        when(shield.isOn()).thenReturn(true);
        when(shield.getRadius()).thenReturn(shieldRadius);
        return ship;
    }

    @Test
    void refractionStartsAtTheImpactPointWhenTheNextTargetIsAwayFromTheHitShip() {
        ShipAPI hitShip = shieldedShipAtOrigin(100f);

        Vector2f origin = Refraction.origin(hitShip, new Vector2f(100f, 0f), new Vector2f(500f, 0f));

        assertEquals(new Vector2f(100f, 0f), origin);
    }

    @Test
    void refractionStartsBeyondTheHitShipsFarSideWhenTheNextTargetIsBehindIt() {
        ShipAPI hitShip = shieldedShipAtOrigin(100f);

        Vector2f origin = Refraction.origin(hitShip, new Vector2f(100f, 0f), new Vector2f(-500f, 0f));

        assertEquals(-110f, origin.x, 0.01f);
        assertEquals(0f, origin.y, 0.01f);
    }

    @Test
    void refractionUsesTheHullRadiusWhenTheShieldIsDown() {
        ShipAPI hitShip = mock(ShipAPI.class);
        when(hitShip.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(0f, 0f));
        when(hitShip.getCollisionRadius()).thenReturn(50f);

        Vector2f origin = Refraction.origin(hitShip, new Vector2f(50f, 0f), new Vector2f(-500f, 0f));

        assertEquals(-60f, origin.x, 0.01f);
    }
}
