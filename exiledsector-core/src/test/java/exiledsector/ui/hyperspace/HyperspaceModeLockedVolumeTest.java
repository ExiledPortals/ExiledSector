package exiledsector.ui.hyperspace;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperspaceModeLockedVolumeTest {

    @Test
    void aDecorationWithNoVolumeIsNeverLockedEvenAgainstAnImmutableSet() {
        assertFalse(HyperspaceMode.isLocked(Set.of(), null));
        assertFalse(HyperspaceMode.isLocked(Set.of("threat"), null));
    }

    @Test
    void aVolumeIsLockedOnlyWhenTheSetNamesIt() {
        assertTrue(HyperspaceMode.isLocked(Set.of("threat"), "threat"));
        assertFalse(HyperspaceMode.isLocked(Set.of("threat"), "core"));
    }
}
