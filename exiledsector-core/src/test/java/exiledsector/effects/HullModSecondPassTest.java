package exiledsector.effects;

import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// java:S5786: the public lookup only reaches a public interface inside a public class
@SuppressWarnings("java:S5786")
public class HullModSecondPassTest {

    public interface SecondPassEffect {
        void applyEffectsAfterShipCreation2ndPass(ShipAPI ship, String id);
    }

    @Test
    void aGameVersionWithTheSecondPassForwardsItToTheWrappedEffect() {
        MethodHandle secondPass = HullModSecondPass.resolve(SecondPassEffect.class);
        SecondPassEffect effect = mock(SecondPassEffect.class);
        ShipAPI ship = mock(ShipAPI.class);

        assertNotNull(secondPass);
        HullModSecondPass.invoke(secondPass, effect, ship, "hullmod_id");

        verify(effect).applyEffectsAfterShipCreation2ndPass(ship, "hullmod_id");
    }

    @Test
    void aGameVersionWithoutTheSecondPassSkipsIt() {
        assertNull(HullModSecondPass.resolve(Runnable.class));
        assertDoesNotThrow(() -> HullModSecondPass.invoke(null, mock(SecondPassEffect.class), mock(ShipAPI.class), "hullmod_id"));
        assertDoesNotThrow(() -> HullModSecondPass.apply(mock(HullModEffect.class), mock(ShipAPI.class), "hullmod_id"));
    }

    @Test
    void aFailureInTheWrappedSecondPassReachesTheCaller() {
        MethodHandle secondPass = HullModSecondPass.resolve(SecondPassEffect.class);
        IllegalArgumentException failure = new IllegalArgumentException("broken hull mod");
        SecondPassEffect effect = (ship, id) -> {
            throw failure;
        };

        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> HullModSecondPass.invoke(secondPass, effect, mock(ShipAPI.class), "hullmod_id")));
    }
}
