package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaperTest {

    private static final float EPSILON = 1e-6f;

    @Test
    void theShareFallsEvenlyFromFullToNothing() {
        Taper taper = new Taper();
        taper.start(4f);
        assertEquals(1f, taper.share(), EPSILON);

        taper.advance(2f);
        assertEquals(0.5f, taper.share(), EPSILON);
        assertTrue(taper.isRunning());

        taper.advance(5f);
        assertEquals(0f, taper.share(), EPSILON);
        assertFalse(taper.isRunning());
    }

    @Test
    void anUnstartedOrStoppedTaperHasNoShare() {
        Taper taper = new Taper();
        assertEquals(0f, taper.share(), EPSILON);

        taper.start(3f);
        taper.stop();

        assertEquals(0f, taper.share(), EPSILON);
        assertFalse(taper.isRunning());
    }

    @Test
    void startingAgainRestartsAtFull() {
        Taper taper = new Taper();
        taper.start(4f);
        taper.advance(3f);

        taper.start(2f);

        assertEquals(1f, taper.share(), EPSILON);
        taper.advance(1f);
        assertEquals(0.5f, taper.share(), EPSILON);
    }

    @Test
    void aTaperedBonusAppliesItsShareEachAdvanceAndNothingOnceStopped() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat timeMult = new MutableStat(1f);
        when(stats.getTimeMult()).thenReturn(timeMult);
        ScaledBonus bonus = new ScaledBonus(ScaledBonus.fixed(StatTarget.liveStat(MutableShipStatsAPI::getTimeMult), StatMode.PERCENT, 40f));
        TaperedBonus tapered = new TaperedBonus(bonus, stats, "taper");

        tapered.start(4f);
        tapered.advance(1f);
        assertEquals(1.3f, timeMult.getModifiedValue(), EPSILON);

        tapered.stop();
        assertEquals(1f, timeMult.getModifiedValue(), EPSILON);
    }
}
