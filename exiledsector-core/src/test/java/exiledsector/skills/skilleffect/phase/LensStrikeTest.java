package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LensStrikeTest {

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        when(ship.isTargetable()).thenReturn(true);
        return ship;
    }

    private static MissileAPI missile(int owner) {
        MissileAPI missile = mock(MissileAPI.class);
        when(missile.getOwner()).thenReturn(owner);
        when(missile.getCollisionClass()).thenReturn(CollisionClass.MISSILE_FF);
        return missile;
    }

    @Test
    void enemyShipsAndMissilesCanBeStruck() {
        ShipAPI striker = ship(0);

        assertTrue(LensStrike.isStrikeable(striker, ship(1)));
        assertTrue(LensStrike.isStrikeable(striker, missile(1)));
    }

    @Test
    void alliesPhasedShipsHulksAndOtherObjectsAreSkipped() {
        ShipAPI striker = ship(0);
        ShipAPI phased = ship(1);
        when(phased.isPhased()).thenReturn(true);
        ShipAPI hulk = ship(1);
        when(hulk.isHulk()).thenReturn(true);
        MissileAPI noCollision = missile(1);
        when(noCollision.getCollisionClass()).thenReturn(CollisionClass.NONE);
        CombatEntityAPI asteroid = mock(CombatEntityAPI.class);
        when(asteroid.getOwner()).thenReturn(1);
        when(asteroid.getCollisionClass()).thenReturn(CollisionClass.ASTEROID);

        assertFalse(LensStrike.isStrikeable(striker, ship(0)));
        assertFalse(LensStrike.isStrikeable(striker, missile(0)));
        assertFalse(LensStrike.isStrikeable(striker, phased));
        assertFalse(LensStrike.isStrikeable(striker, hulk));
        assertFalse(LensStrike.isStrikeable(striker, noCollision));
        assertFalse(LensStrike.isStrikeable(striker, asteroid));
    }

    @Test
    void closerTargetsWeighMoreBeyondTheCloseDistance() {
        assertEquals(1f, LensStrike.weight(0f));
        assertEquals(1f, LensStrike.weight(100f));
        assertEquals(0.25f, LensStrike.weight(400f));
    }
}
