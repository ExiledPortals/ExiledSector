package exiledsector.ui.atlas;

import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StormWeatherTest {

    private static final float SLOT_SECONDS = 1f;
    private static final int SEED = 137;

    @Test
    void anUntakenStormIsSlowerFainterAndFlashesLessOftenThanATakenOne() {
        assertTrue(StormWeather.pace(Lighting.LIT) > StormWeather.pace(Lighting.AVAILABLE));
        assertTrue(StormWeather.pace(Lighting.AVAILABLE) > StormWeather.pace(Lighting.DULL));
        assertTrue(StormWeather.pace(Lighting.DULL) > 0f);
        assertEquals(1f, StormWeather.pace(Lighting.LIT));
        assertTrue(StormWeather.flashChance(Lighting.LIT) > StormWeather.flashChance(Lighting.AVAILABLE));
        assertTrue(StormWeather.flashChance(Lighting.AVAILABLE) > StormWeather.flashChance(Lighting.DULL));
        assertTrue(StormWeather.cloudShare(Lighting.LIT) > StormWeather.cloudShare(Lighting.DULL));
        assertTrue(StormWeather.flashShare(Lighting.LIT) > StormWeather.flashShare(Lighting.DULL));
    }

    @Test
    void aFlashStrikesAtFullStrengthAndFadesOutEarlyInItsSlot() {
        float slotStart = nextSlotStart();
        float strike = StormWeather.flash(SEED, slotStart + 0.0001f, SLOT_SECONDS, 1f);
        float fading = StormWeather.flash(SEED, slotStart + SLOT_SECONDS * StormWeather.FLASH_SHARE_OF_SLOT / 2f, SLOT_SECONDS, 1f);
        float gone = StormWeather.flash(SEED, slotStart + SLOT_SECONDS * (StormWeather.FLASH_SHARE_OF_SLOT + 0.05f), SLOT_SECONDS, 1f);

        assertTrue(strike > 0.99f, "strike " + strike);
        assertTrue(fading > 0f && fading < strike, "fading " + fading);
        assertEquals(0f, gone);
    }

    @Test
    void noChanceMeansNoFlashAndAFlashIsTheSameEveryTimeTheSameMomentIsDrawn() {
        for (float seconds = 0f; seconds < 50f; seconds += 0.13f) {
            assertEquals(0f, StormWeather.flash(SEED, seconds, SLOT_SECONDS, 0f));
            assertEquals(StormWeather.flash(SEED, seconds, SLOT_SECONDS, 0.5f), StormWeather.flash(SEED, seconds, SLOT_SECONDS, 0.5f));
        }
    }

    @Test
    void aDullStormFlashesInFarFewerSlotsThanATakenOne() {
        int litFlashes = flashingSlots(StormWeather.flashChance(Lighting.LIT));
        int dullFlashes = flashingSlots(StormWeather.flashChance(Lighting.DULL));

        assertTrue(litFlashes > dullFlashes * 3, litFlashes + " lit against " + dullFlashes + " dull");
        assertTrue(dullFlashes > 0);
    }

    @Test
    void differentNodesFlashOnDifferentBeats() {
        int shared = 0;
        int slots = 400;
        for (int slot = 0; slot < slots; slot++) {
            boolean first = StormWeather.unit(1, slot, 0) < 0.5f;
            boolean second = StormWeather.unit(2, slot, 0) < 0.5f;
            if (first == second) {
                shared++;
            }
        }

        assertTrue(shared < slots * 0.75f, shared + " of " + slots);
    }

    @Test
    void picksAndUnitsStayInRange() {
        for (int slot = -50; slot < 500; slot++) {
            int picked = StormWeather.pick(SEED, slot, 5);
            float unit = StormWeather.unit(SEED, slot, 3);
            float signed = StormWeather.signedUnit(SEED, slot, 4);
            assertTrue(picked >= 0 && picked < 5);
            assertTrue(unit >= 0f && unit < 1f);
            assertTrue(signed >= -1f && signed < 1f);
        }
        assertEquals(0, StormWeather.pick(SEED, 3, 0));
    }

    private static float nextSlotStart() {
        int firstSlot = StormWeather.slotOf(SEED, 0f, SLOT_SECONDS);
        float low = 0f;
        float high = SLOT_SECONDS;
        for (int step = 0; step < 40; step++) {
            float middle = (low + high) / 2f;
            if (StormWeather.slotOf(SEED, middle, SLOT_SECONDS) == firstSlot) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    private static int flashingSlots(float chance) {
        float firstStart = nextSlotStart();
        int flashes = 0;
        for (int slot = 0; slot < 1000; slot++) {
            if (StormWeather.flash(SEED, firstStart + slot * SLOT_SECONDS + 0.01f, SLOT_SECONDS, chance) > 0f) {
                flashes++;
            }
        }
        return flashes;
    }
}
