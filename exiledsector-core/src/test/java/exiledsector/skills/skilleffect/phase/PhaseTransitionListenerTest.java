package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseTransitionListenerTest {

    private static final class RecordingListener extends PhaseTransitionListener {

        private final List<String> transitions = new ArrayList<>();

        RecordingListener(ShipAPI ownerShip) {
            super(ownerShip);
        }

        @Override
        void onPhaseEntry() {
            transitions.add("entry");
        }

        @Override
        void onPhaseExit() {
            transitions.add("exit");
        }
    }

    @Test
    void eachFlipIntoOrOutOfPhaseFiresOnce() {
        ShipAPI ship = mock(ShipAPI.class);
        RecordingListener listener = new RecordingListener(ship);

        for (boolean phased : new boolean[]{false, true, true, true, false, false, true}) {
            when(ship.isPhased()).thenReturn(phased);
            listener.advance(0.1f);
        }

        assertEquals(List.of("entry", "exit", "entry"), listener.transitions);
    }
}
