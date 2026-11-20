package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Nebula;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NebulaWeatherTest {

    private static final float FRAME_SECONDS = 0.02f;
    private static final List<NebulaWeather.Style> STYLES = List.of(NebulaWeather.PUFF, NebulaWeather.TRAILS, NebulaWeather.SETTLING,
            NebulaWeather.IONIZED);

    @Test
    void everyPuffStaysWhollyInsideItsNodeWhereverItDrifts() {
        NebulaWeather.PuffPlace place = new NebulaWeather.PuffPlace();
        for (NebulaWeather.Style style : STYLES) {
            for (int seed = 0; seed < 360; seed += 7) {
                for (int puff = 0; puff < style.puffs(); puff++) {
                    for (float seconds = 0f; seconds < 200f; seconds += 1.7f) {
                        NebulaWeather.place(style, seed, puff, seconds, place);
                        float reach = (float) Math.hypot(place.offsetX, place.offsetY);
                        assertTrue(reach + place.halfSize <= 1.0001f,
                                "seed " + seed + " puff " + puff + " at " + seconds + " reaches " + (reach + place.halfSize));
                        assertTrue(place.halfSize > 0f);
                        assertTrue(place.presence >= 0f && place.presence <= 1f);
                        assertTrue(place.cell >= 0 && place.cell < NebulaWeather.SHEET_CELLS);
                    }
                }
            }
        }
    }

    @Test
    void backPuffsAreBiggerThanFrontOnesAndNotablesStackMorePuffsThanSmallNodes() {
        NebulaWeather.PuffPlace place = new NebulaWeather.PuffPlace();
        for (NebulaWeather.Style style : STYLES) {
            NebulaWeather.place(style, 40, 0, 0f, place);
            float back = place.halfSize;
            NebulaWeather.place(style, 40, style.puffs() - 1, 0f, place);
            assertTrue(back > place.halfSize, back + " against " + place.halfSize);
        }
        assertTrue(NebulaWeather.TRAILS.puffs() > NebulaWeather.PUFF.puffs());
        assertTrue(NebulaWeather.SETTLING.puffs() > NebulaWeather.PUFF.puffs());
        assertTrue(NebulaWeather.IONIZED.puffs() > NebulaWeather.PUFF.puffs());
    }

    @Test
    void thePuffsKeepDriftingSoTheStackNeverFreezes() {
        NebulaWeather.PuffPlace place = new NebulaWeather.PuffPlace();
        for (NebulaWeather.Style style : STYLES) {
            NebulaWeather.place(style, 90, 1, 10f, place);
            float fromX = place.offsetX;
            float fromY = place.offsetY;
            float fromTurn = place.turnDegrees;
            NebulaWeather.place(style, 90, 1, 14f, place);

            assertTrue(Math.hypot(place.offsetX - fromX, place.offsetY - fromY) > 0.005f);
            assertTrue(place.turnDegrees != fromTurn);
        }
    }

    @Test
    void trailAndSettlingPuffsFadeAtTheEndsOfTheirJourney() {
        NebulaWeather.PuffPlace place = new NebulaWeather.PuffPlace();
        for (NebulaWeather.Style style : List.of(NebulaWeather.TRAILS, NebulaWeather.SETTLING)) {
            float faintest = 1f;
            float fullest = 0f;
            for (float seconds = 0f; seconds < style.cycleSeconds(); seconds += 0.1f) {
                NebulaWeather.place(style, 15, 0, seconds, place);
                faintest = Math.min(faintest, place.presence);
                fullest = Math.max(fullest, place.presence);
            }
            assertTrue(faintest < 0.05f, "faintest " + faintest);
            assertTrue(fullest > 0.95f, "fullest " + fullest);
        }
    }

    @Test
    void aFlashFlickersSeveralTimesThenGoesDark() {
        boolean flickered = false;
        for (int slot = 0; slot < 50 && !flickered; slot++) {
            int rises = 0;
            float previous = NebulaWeather.flicker(77, slot, 0f);
            assertEquals(1f, previous, 0.0001f);
            for (float progress = 0.005f; progress < 1f; progress += 0.005f) {
                float brightness = NebulaWeather.flicker(77, slot, progress);
                assertTrue(brightness >= 0f && brightness <= 1f);
                if (brightness > previous + 0.01f) {
                    rises++;
                }
                previous = brightness;
            }
            flickered = rises > 0;
        }

        assertTrue(flickered);
        assertEquals(0f, NebulaWeather.flicker(77, 3, -0.1f));
        assertEquals(0f, NebulaWeather.flicker(77, 3, 1f));
    }

    @Test
    void untakenNebulaeFlashLessOftenAndIonizedDustFlashesMostOften() {
        int lit = flashesSeen(NebulaWeather.PUFF, Lighting.LIT);
        int available = flashesSeen(NebulaWeather.PUFF, Lighting.AVAILABLE);
        int dull = flashesSeen(NebulaWeather.PUFF, Lighting.DULL);
        int ionized = flashesSeen(NebulaWeather.IONIZED, Lighting.LIT);
        int settling = flashesSeen(NebulaWeather.SETTLING, Lighting.LIT);

        assertTrue(lit > available && available > dull, lit + " / " + available + " / " + dull);
        assertTrue(dull > 0);
        assertTrue(ionized > settling, ionized + " ionized against " + settling + " settling");
    }

    @Test
    void theFlashStreamsOfANotableKeepTheirOwnBeat() {
        int shared = 0;
        int sampled = 0;
        float chance = NebulaWeather.flashChance(NebulaWeather.TRAILS, StormWeather.flashChance(Lighting.LIT));
        for (float seconds = 0f; seconds < 300f; seconds += 0.37f) {
            boolean first = NebulaWeather.flashProgress(NebulaWeather.TRAILS, NebulaWeather.streamSeed(5, 0), seconds, chance) >= 0f;
            boolean second = NebulaWeather.flashProgress(NebulaWeather.TRAILS, NebulaWeather.streamSeed(5, 1), seconds, chance) >= 0f;
            if (first == second) {
                shared++;
            }
            sampled++;
        }

        assertTrue(shared < sampled * 0.9f, shared + " of " + sampled);
    }

    @Test
    void everyNebulaHasItsOwnLookAndNoStyleFallsBackToAPlainPuff() {
        assertSame(NebulaWeather.PUFF, NebulaWeather.styleOf(Nebula.NONE));
        assertSame(NebulaWeather.PUFF, NebulaWeather.styleOf(Nebula.PUFF));
        assertSame(NebulaWeather.TRAILS, NebulaWeather.styleOf(Nebula.TRAILS));
        assertSame(NebulaWeather.SETTLING, NebulaWeather.styleOf(Nebula.SETTLING));
        assertSame(NebulaWeather.IONIZED, NebulaWeather.styleOf(Nebula.IONIZED));
        assertTrue(NebulaWeather.flashChance(NebulaWeather.IONIZED, 1f) <= 1f);
    }

    private static int flashesSeen(NebulaWeather.Style style, Lighting lighting) {
        float chance = NebulaWeather.flashChance(style, StormWeather.flashChance(lighting));
        int flashes = 0;
        boolean[] flashing = new boolean[style.flashStreams()];
        for (float seconds = 0f; seconds < 1200f; seconds += FRAME_SECONDS) {
            for (int stream = 0; stream < style.flashStreams(); stream++) {
                boolean now = NebulaWeather.flashProgress(style, NebulaWeather.streamSeed(31, stream), seconds, chance) >= 0f;
                if (now && !flashing[stream]) {
                    flashes++;
                }
                flashing[stream] = now;
            }
        }
        return flashes;
    }
}
