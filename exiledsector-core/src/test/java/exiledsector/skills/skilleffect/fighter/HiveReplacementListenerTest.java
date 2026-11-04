package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HiveReplacementListenerTest {

    @Test
    void theFragmentsRestoreTheirShareOfTheLostRate() {
        assertEquals(0.7f, HiveReplacementListener.restoredRate(0.6f, 0.1f, 100f), 0.0001f);
        assertEquals(0.65f, HiveReplacementListener.restoredRate(0.6f, 0.1f, 50f), 0.0001f);
        assertEquals(0.7f, HiveReplacementListener.restoredRate(0.6f, 0.1f, 150f), 0.0001f);
    }

    @Test
    void withoutASwarmTheReplacementRateDropsAsNormal() {
        FighterLaunchBayAPI bay = mock(FighterLaunchBayAPI.class);
        when(bay.getCurrRate()).thenReturn(1f, 0.9f);
        ShipAPI carrier = mock(ShipAPI.class);
        when(carrier.getLaunchBaysCopy()).thenReturn(List.of(bay));
        when(carrier.isAlive()).thenReturn(true);
        HiveReplacementListener listener = new HiveReplacementListener(carrier);

        listener.advance(0.1f);
        listener.advance(0.1f);

        verify(bay, never()).setCurrRate(anyFloat());
    }
}
