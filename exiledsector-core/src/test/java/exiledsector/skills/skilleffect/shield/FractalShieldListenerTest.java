package exiledsector.skills.skilleffect.shield;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FractalShieldListenerTest {

    @Test
    void theAbsorbedDamageIsWhatTheReductionKeptOffTheShield() {
        assertEquals(15f, FractalShieldListener.absorbedDamage(85f, 15f), 0.001f);
        assertEquals(10f, FractalShieldListener.absorbedDamage(90f, 10f), 0.001f);
    }

    @Test
    void noReductionAbsorbsNothing() {
        assertEquals(0f, FractalShieldListener.absorbedDamage(100f, 0f));
        assertEquals(0f, FractalShieldListener.absorbedDamage(100f, 100f));
    }
}
