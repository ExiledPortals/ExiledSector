package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VanillaShipViewTest {

    static final class ObfuscatedView {
    }

    public static final class Engine {
        public void doNotCleanupThisFrame(ObfuscatedView view) {
            view.hashCode();
        }
    }

    public static final class OverloadedEngine {
        public void doNotCleanupThisFrame(ObfuscatedView view) {
            view.hashCode();
        }

        public void doNotCleanupThisFrame(String view) {
            view.hashCode();
        }
    }

    @Test
    void theViewClassIsTheOneTheEngineKeepsAliveWhateverItsObfuscatedName() throws Throwable {
        assertEquals(ObfuscatedView.class, VanillaShipView.keptViewClass(Engine.class));
    }

    @Test
    void anEngineWithoutASingleKeepAliveMethodLeavesTheViewUnavailable() {
        assertThrows(NoSuchMethodException.class, () -> VanillaShipView.keptViewClass(Object.class));
        assertThrows(NoSuchMethodException.class, () -> VanillaShipView.keptViewClass(OverloadedEngine.class));
    }
}
