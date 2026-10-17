package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WingNumbersDamageListenerTest {

    @Test
    void eachLivingFighterInTheWingCutsTheDamageTakenFurther() {
        assertEquals(0.9f, WingNumbersDamageListener.damageTakenMult(10f, 1), 0.0001f);
        assertEquals(0.7f, WingNumbersDamageListener.damageTakenMult(10f, 3), 0.0001f);
    }

    @Test
    void aHugeWingStopsAtTheCapInsteadOfBecomingInvulnerable() {
        assertEquals(1f - WingNumbersDamageListener.MAX_REDUCTION_PERCENT / 100f, WingNumbersDamageListener.damageTakenMult(10f, 12), 0.0001f);
    }

    @Test
    void anEmptyWingTakesFullDamage() {
        assertEquals(1f, WingNumbersDamageListener.damageTakenMult(10f, 0), 0.0001f);
    }
}
