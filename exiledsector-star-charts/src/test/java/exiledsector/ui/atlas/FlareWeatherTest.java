package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Flare;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.decoration.ConvoyCrossing;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlareWeatherTest {

    private static final int SEED = 211;
    private static final float FRAME_SECONDS = 0.02f;
    private static final float WATCH_SECONDS = 1200f;

    @Test
    void solarMaximumFlaresMostOftenAndPerfectLightLeastOften() {
        int often = flaresSeen(FlareWeather.OFTEN, Lighting.LIT);
        int plain = flaresSeen(FlareWeather.PLAIN, Lighting.LIT);
        int gentle = flaresSeen(FlareWeather.GENTLE, Lighting.LIT);
        int perfect = flaresSeen(FlareWeather.PERFECT, Lighting.LIT);

        assertTrue(often > plain * 3, often + " often against " + plain + " plain");
        assertTrue(plain > gentle, plain + " plain against " + gentle + " gentle");
        assertTrue(gentle > perfect, gentle + " gentle against " + perfect + " perfect");
        assertTrue(perfect > 0);
    }

    @Test
    void anUntakenStarFlaresLessOftenAndMoreFaintlyThanATakenOne() {
        int lit = flaresSeen(FlareWeather.PLAIN, Lighting.LIT);
        int available = flaresSeen(FlareWeather.PLAIN, Lighting.AVAILABLE);
        int dull = flaresSeen(FlareWeather.PLAIN, Lighting.DULL);

        assertTrue(lit > available && available > dull, lit + " / " + available + " / " + dull);
        assertTrue(dull > 0);
        assertEquals(1f, FlareWeather.glowShare(Lighting.LIT));
        assertTrue(FlareWeather.glowShare(Lighting.LIT) > FlareWeather.glowShare(Lighting.AVAILABLE));
        assertTrue(FlareWeather.glowShare(Lighting.AVAILABLE) > FlareWeather.glowShare(Lighting.DULL));
        assertTrue(FlareWeather.glowShare(Lighting.DULL) > 0f);
    }

    @Test
    void aFlareInProgressCarriesOnSmoothlyWhenTheNodeIsTaken() {
        float flareSeconds = firstFlareStart(FlareWeather.WARM, 0) + 0.3f;
        float before = FlareWeather.progress(FlareWeather.streamSeed(SEED, 0), flareSeconds, FlareWeather.WARM);
        float step = FRAME_SECONDS * StormWeather.pace(Lighting.LIT);
        float after = FlareWeather.progress(FlareWeather.streamSeed(SEED, 0), flareSeconds + step, FlareWeather.WARM);
        float burstSeconds = FlareWeather.WARM.slotSeconds() * FlareWeather.WARM.burstShare();

        assertTrue(before >= 0f && after > before, before + " then " + after);
        assertEquals(step / burstSeconds, after - before, 0.001f);
    }

    @Test
    void theSameMomentAlwaysDrawsTheSameFlareAndEachStreamKeepsItsOwnBeat() {
        int shared = 0;
        int sampled = 0;
        for (float seconds = 0f; seconds < 300f; seconds += 0.37f) {
            int first = FlareWeather.streamSeed(SEED, 0);
            int second = FlareWeather.streamSeed(SEED, 1);
            assertEquals(FlareWeather.progress(first, seconds, FlareWeather.OFTEN), FlareWeather.progress(first, seconds, FlareWeather.OFTEN));
            boolean firstFlaring = FlareWeather.progress(first, seconds, FlareWeather.OFTEN) >= 0f;
            boolean secondFlaring = FlareWeather.progress(second, seconds, FlareWeather.OFTEN) >= 0f;
            if (firstFlaring == secondFlaring) {
                shared++;
            }
            sampled++;
        }

        assertTrue(shared < sampled * 0.9f, shared + " of " + sampled);
    }

    @Test
    void perfectFlaresStepRoundTheStarByTheGoldenAngle() {
        int streamSeed = FlareWeather.streamSeed(SEED, 0);
        for (int slot = 0; slot < 20; slot++) {
            float angle = FlareWeather.angleDegrees(Flare.PERFECT, streamSeed, slot, 0f);
            float next = FlareWeather.angleDegrees(Flare.PERFECT, streamSeed, slot + 1, 0f);
            assertEquals(FlareWeather.GOLDEN_ANGLE_DEGREES, next - angle, 0.01f);
        }
    }

    @Test
    void lickingFlaresReachOutTowardTheConvoyLaneAndPastIt() {
        float headingDegrees = -40f;
        float laneDegrees = headingDegrees + ConvoyCrossing.LANE_SIDE_DEGREES;
        int streamSeed = FlareWeather.streamSeed(SEED, 1);
        for (int slot = -20; slot < 200; slot++) {
            float angle = FlareWeather.angleDegrees(Flare.LICKING, streamSeed, slot, headingDegrees);
            assertTrue(Math.abs(angle - laneDegrees) <= FlareWeather.LICK_SPREAD_DEGREES, "slot " + slot + " at " + angle);
        }

        assertTrue(1f + FlareWeather.LICKING.rise().lift() > ConvoyCrossing.LANE_SHARE);
    }

    @Test
    void otherFlaresLeaveFromAllRoundTheStar() {
        Set<Integer> quarters = new HashSet<>();
        int streamSeed = FlareWeather.streamSeed(SEED, 0);
        for (int slot = 0; slot < 40; slot++) {
            float angle = FlareWeather.angleDegrees(Flare.PLAIN, streamSeed, slot, 0f);
            assertTrue(angle >= 0f && angle < 360f);
            quarters.add((int) (angle / 90f));
        }

        assertEquals(4, quarters.size());
    }

    @Test
    void everyFlareStyleHasItsOwnLookAndNoFlareFallsBackToThePlainOne() {
        assertSame(FlareWeather.PLAIN, FlareWeather.styleOf(Flare.NONE));
        assertSame(FlareWeather.PLAIN, FlareWeather.styleOf(Flare.PLAIN));
        assertSame(FlareWeather.OFTEN, FlareWeather.styleOf(Flare.OFTEN));
        assertSame(FlareWeather.WARM, FlareWeather.styleOf(Flare.WARM));
        assertSame(FlareWeather.GENTLE, FlareWeather.styleOf(Flare.GENTLE));
        assertSame(FlareWeather.PERFECT, FlareWeather.styleOf(Flare.PERFECT));
        assertSame(FlareWeather.LICKING, FlareWeather.styleOf(Flare.LICKING));
        assertTrue(FlareWeather.PERFECT.arched());
        assertNull(FlareWeather.PERFECT.rise());
        assertNotNull(FlareWeather.GENTLE.rise());
        assertTrue(FlareWeather.GENTLE.rise().lift() < FlareWeather.PLAIN.rise().lift());
        assertTrue(FlareWeather.GENTLE.burstShare() * FlareWeather.GENTLE.slotSeconds()
                > FlareWeather.OFTEN.burstShare() * FlareWeather.OFTEN.slotSeconds());
    }

    @Test
    void thePlainStyleTakesTheAnomalyColourAndTheOthersKeepTheirOwn() {
        Color anomaly = new Color(255, 228, 160);

        assertSame(anomaly, FlareWeather.PLAIN.colourOr(anomaly));
        assertSame(FlareWeather.WARM.colour(), FlareWeather.WARM.colourOr(anomaly));
    }

    @Test
    void theCoronaShimmerStaysBetweenNothingAndFull() {
        for (float seconds = 0f; seconds < 200f; seconds += 0.11f) {
            float shimmer = FlareWeather.shimmer(SEED, seconds);
            assertTrue(shimmer >= 0f && shimmer <= 1f, "at " + seconds + ": " + shimmer);
        }
    }

    private static int flaresSeen(FlareWeather.Style style, Lighting lighting) {
        int flares = 0;
        float flareSeconds = 0f;
        boolean[] flaring = new boolean[style.streams()];
        for (float realSeconds = 0f; realSeconds < WATCH_SECONDS; realSeconds += FRAME_SECONDS) {
            flareSeconds += FRAME_SECONDS * StormWeather.pace(lighting);
            for (int stream = 0; stream < style.streams(); stream++) {
                boolean now = FlareWeather.progress(FlareWeather.streamSeed(SEED, stream), flareSeconds, style) >= 0f;
                if (now && !flaring[stream]) {
                    flares++;
                }
                flaring[stream] = now;
            }
        }
        return flares;
    }

    private static float firstFlareStart(FlareWeather.Style style, int stream) {
        int streamSeed = FlareWeather.streamSeed(SEED, stream);
        boolean wasFlaring = true;
        for (float seconds = 0f; seconds < WATCH_SECONDS; seconds += FRAME_SECONDS) {
            boolean flaring = FlareWeather.progress(streamSeed, seconds, style) >= 0f;
            if (flaring && !wasFlaring) {
                return seconds;
            }
            wasFlaring = flaring;
        }
        throw new AssertionError("no flare");
    }
}
