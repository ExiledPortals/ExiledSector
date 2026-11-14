package exiledsector.charts.atlas;

import exiledsector.charts.atlas.TestBorders.Border;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasProgressTest {

    @Test
    void completingASystemReleasesItUnlessItsNewHolderTookItDuringTheRun() {
        AtlasState releasedState = new AtlasState(TestBorders.SEED);
        AtlasGenerator releasedGenerator = TestBorders.generator(releasedState);
        Border released = TestBorders.border(releasedGenerator, releasedState, "tritachyon", "hegemony");

        AtlasProgress.complete(releasedGenerator, releasedState, released.contested(), 1);

        assertTrue(releasedState.isCompleted(released.contested()));
        assertFalse(releasedState.isClaimed(released.contested()));

        AtlasState takenState = new AtlasState(TestBorders.SEED);
        AtlasGenerator takenGenerator = TestBorders.generator(takenState);
        Border taken = TestBorders.border(takenGenerator, takenState, "tritachyon", "hegemony");
        takenState.claim(taken.contested(), taken.attacker().commandCentre());

        AtlasProgress.complete(takenGenerator, takenState, taken.contested(), 1, true);

        assertTrue(takenState.isCompleted(taken.contested()));
        assertEquals(taken.attacker().commandCentre(), takenState.commandCentreOf(taken.contested()));
    }
}
