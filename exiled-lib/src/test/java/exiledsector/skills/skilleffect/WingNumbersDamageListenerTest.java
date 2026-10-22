package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private static ShipAPI flyingFighter() {
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.isAlive()).thenReturn(true);
        when(fighter.getHitpoints()).thenReturn(100f);
        return fighter;
    }

    @Test
    void onlyFightersOutWithTheWingCountSoDockedLandingAndLaunchingOnesDoNot() {
        ShipAPI hit = flyingFighter();
        ShipAPI flying = flyingFighter();
        ShipAPI phased = flyingFighter();
        when(phased.isPhased()).thenReturn(true);
        ShipAPI docked = flyingFighter();
        when(docked.isFinishedLanding()).thenReturn(true);
        ShipAPI landing = flyingFighter();
        when(landing.isLanding()).thenReturn(true);
        ShipAPI liftingOff = flyingFighter();
        when(liftingOff.isLiftingOff()).thenReturn(true);
        ShipAPI destroyed = flyingFighter();
        when(destroyed.isAlive()).thenReturn(false);
        ShipAPI removed = flyingFighter();
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(wing.getWingMembers()).thenReturn(List.of(hit, flying, phased, docked, landing, liftingOff, destroyed, removed));
        when(hit.getWing()).thenReturn(wing);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.isEntityInPlay(any())).thenReturn(true);
        when(engine.isEntityInPlay(removed)).thenReturn(false);

        assertEquals(3, WingNumbersDamageListener.fightersOutWithWing(hit, engine), "the struck fighter, the flying one and the phased one");
    }

    @Test
    void aFighterWithoutAWingCountsOnlyItself() {
        assertEquals(1, WingNumbersDamageListener.fightersOutWithWing(flyingFighter(), null));
    }
}
