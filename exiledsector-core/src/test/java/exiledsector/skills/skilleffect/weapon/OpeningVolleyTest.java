package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpeningVolleyTest {

    private static ShipAPI deployedFor(float seconds) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getFullTimeDeployed()).thenReturn(seconds);
        return ship;
    }

    @Test
    void theVolleyLastsForTheFirstSecondsAfterDeploying() {
        assertTrue(OpeningVolley.isUnderway(deployedFor(0f)));
        assertTrue(OpeningVolley.isUnderway(deployedFor(OpeningVolley.SECONDS - 0.1f)));
        assertFalse(OpeningVolley.isUnderway(deployedFor(OpeningVolley.SECONDS)));
        assertFalse(OpeningVolley.isUnderway(deployedFor(120f)));
    }
}
