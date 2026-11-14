package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseCloakTest {

    private static ShipSystemAPI system(boolean phaseCloak) {
        ShipSystemSpecAPI spec = mock(ShipSystemSpecAPI.class);
        when(spec.isPhaseCloak()).thenReturn(phaseCloak);
        ShipSystemAPI system = mock(ShipSystemAPI.class);
        when(system.getSpecAPI()).thenReturn(spec);
        return system;
    }

    @Test
    void theDefensePhaseCloakComesFirst() {
        ShipAPI ship = mock(ShipAPI.class);
        ShipSystemAPI defenseCloak = system(true);
        ShipSystemAPI phaseShipSystem = system(true);
        when(ship.getPhaseCloak()).thenReturn(defenseCloak);
        when(ship.getSystem()).thenReturn(phaseShipSystem);

        assertSame(defenseCloak, PhaseCloak.of(ship));
    }

    @Test
    void aShipSystemThatIsAPhaseCloakStandsInForAMissingDefenseCloak() {
        ShipAPI ship = mock(ShipAPI.class);
        ShipSystemAPI concordShift = system(true);
        when(ship.getSystem()).thenReturn(concordShift);

        assertSame(concordShift, PhaseCloak.of(ship));
    }

    @Test
    void anOrdinaryShipSystemIsNoPhaseCloak() {
        ShipAPI ship = mock(ShipAPI.class);
        ShipSystemAPI ordinarySystem = system(false);
        when(ship.getSystem()).thenReturn(ordinarySystem);

        assertNull(PhaseCloak.of(ship));
        assertNull(PhaseCloak.of(mock(ShipAPI.class)));
    }
}
