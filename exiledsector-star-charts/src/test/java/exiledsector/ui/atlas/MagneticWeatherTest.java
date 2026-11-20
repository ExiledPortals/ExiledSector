package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Field;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagneticWeatherTest {

    private static final int SEED = 137;
    private static final float FRAME_SECONDS = 0.02f;
    private static final List<MagneticWeather.Style> STYLES = List.of(MagneticWeather.PLAIN, MagneticWeather.CHARGED, MagneticWeather.HARVEST,
            MagneticWeather.ION, MagneticWeather.SURGE);

    @Test
    void everyFieldLoopStartsAndEndsOnTheSurfaceAndCrossesTheEquatorAtItsReach() {
        for (MagneticWeather.Style style : STYLES) {
            for (int loop = 0; loop < style.loopsPerSide(); loop++) {
                float reach = MagneticWeather.loopReach(style, loop);
                assertEquals(1f, distance(reach, 0f), 0.001f);
                assertEquals(1f, distance(reach, 1f), 0.001f);
                assertEquals(reach, MagneticWeather.loopAcross(reach, 0.5f), 0.001f);
                assertEquals(0f, MagneticWeather.loopAxial(reach, 0.5f), 0.001f);
            }
        }
    }

    @Test
    void theDotsOfALoopStayOutsideThePlanetAndRunFromOnePoleToTheOther() {
        float reach = MagneticWeather.loopReach(MagneticWeather.CHARGED, 0);
        float previousAxial = Float.MAX_VALUE;
        for (int dot = 0; dot < MagneticWeather.DOTS_PER_LOOP; dot++) {
            float along = MagneticWeather.dotAlong(dot);
            assertTrue(along > 0f && along < 1f);
            assertTrue(distance(reach, along) >= 1f, "dot " + dot + " inside the planet");
            float axial = MagneticWeather.loopAxial(reach, along);
            assertTrue(axial < previousAxial, "dot " + dot + " does not move toward the far pole");
            previousAxial = axial;
        }
    }

    @Test
    void outerLoopsReachFurtherThanInnerOnes() {
        for (MagneticWeather.Style style : STYLES) {
            for (int loop = 1; loop < style.loopsPerSide(); loop++) {
                assertTrue(MagneticWeather.loopReach(style, loop) > MagneticWeather.loopReach(style, loop - 1));
            }
            assertTrue(MagneticWeather.loopReach(style, 0) > 1f);
        }
    }

    @Test
    void aFlowPulseRunsAlongTheLoopAndStaysBetweenNothingAndFull() {
        MagneticWeather.Style style = MagneticWeather.PLAIN;
        float seconds = 0f;
        while (brightestAlong(style, seconds) < 0.2f || brightestAlong(style, seconds) > 0.45f) {
            seconds += FRAME_SECONDS;
            assertTrue(seconds < style.flowSeconds() * 3f, "no pulse");
        }
        float later = seconds + style.flowSeconds() * 0.25f;

        assertEquals(0.25f, brightestAlong(style, later) - brightestAlong(style, seconds), 0.02f);
        for (int step = 0; step <= 100; step++) {
            float glow = MagneticWeather.flowGlow(step / 100f, style.flowSeconds(), later, SEED);
            assertTrue(glow >= 0f && glow <= 1f);
        }
    }

    @Test
    void anIonFrontAuroraSurgesWithEveryStormFlashAndOutlastsIt() {
        float chance = StormWeather.flashChance(Lighting.LIT);
        int flashFrames = 0;
        int surgeFrames = 0;
        for (float seconds = 0f; seconds < 600f; seconds += FRAME_SECONDS) {
            float flash = StormWeather.flash(SEED, seconds, StormWeather.SCATTERED_FLASH_SECONDS, chance);
            float surge = MagneticWeather.stormSurge(SEED, seconds, chance);
            assertTrue(surge >= 0f && surge <= 1f);
            if (flash > 0f) {
                flashFrames++;
                assertTrue(surge >= flash, "at " + seconds + " the surge " + surge + " lags the flash " + flash);
            }
            if (surge > 0f) {
                surgeFrames++;
            }
        }

        assertTrue(flashFrames > 0);
        assertTrue(surgeFrames > flashFrames * 2, surgeFrames + " surging frames against " + flashFrames + " flashing");
    }

    @Test
    void aGeomagneticSurgeAuroraFlaresOnlyWhileTheStarFlares() {
        FlareWeather.Style flare = FlareWeather.SURGE;
        int surging = 0;
        for (float seconds = 0f; seconds < 600f; seconds += FRAME_SECONDS) {
            boolean flaring = false;
            for (int stream = 0; stream < flare.streams(); stream++) {
                flaring |= FlareWeather.progress(FlareWeather.streamSeed(SEED, stream), seconds, flare) > 0f;
            }
            float surge = MagneticWeather.flareSurge(SEED, seconds, flare);
            assertTrue(surge >= 0f && surge <= 1f);
            if (!flaring) {
                assertEquals(0f, surge, "at " + seconds);
            }
            if (surge > 0f) {
                surging++;
            }
        }

        assertTrue(surging > 0);
    }

    @Test
    void theAuroraShimmerNeverFadesOutOrOverflows() {
        for (float seconds = 0f; seconds < 300f; seconds += 0.13f) {
            float shimmer = MagneticWeather.shimmer(SEED, seconds);
            assertTrue(shimmer > 0.3f && shimmer <= 1f, "at " + seconds + ": " + shimmer);
        }
    }

    @Test
    void everyFieldHasItsOwnLookAndNoFieldFallsBackToThePlainOne() {
        assertSame(MagneticWeather.PLAIN, MagneticWeather.styleOf(Field.NONE));
        assertSame(MagneticWeather.PLAIN, MagneticWeather.styleOf(Field.PLAIN));
        assertSame(MagneticWeather.CHARGED, MagneticWeather.styleOf(Field.CHARGED));
        assertSame(MagneticWeather.HARVEST, MagneticWeather.styleOf(Field.HARVEST));
        assertSame(MagneticWeather.ION, MagneticWeather.styleOf(Field.ION));
        assertSame(MagneticWeather.SURGE, MagneticWeather.styleOf(Field.SURGE));
        assertSame(MagneticWeather.Surge.STORM, MagneticWeather.ION.surge());
        assertSame(MagneticWeather.Surge.FLARE, MagneticWeather.SURGE.surge());
        assertTrue(MagneticWeather.CHARGED.flowSeconds() < MagneticWeather.HARVEST.flowSeconds());
        assertTrue(MagneticWeather.CHARGED.sparks());
        assertTrue(MagneticWeather.HARVEST.poleGlows());
    }

    private static float distance(float reach, float along) {
        return (float) Math.hypot(MagneticWeather.loopAcross(reach, along), MagneticWeather.loopAxial(reach, along));
    }

    private static float brightestAlong(MagneticWeather.Style style, float seconds) {
        float brightest = -1f;
        float brightestAlong = 0f;
        for (int step = 0; step <= 200; step++) {
            float along = step / 200f;
            float glow = MagneticWeather.flowGlow(along, style.flowSeconds(), seconds, SEED);
            if (glow > brightest) {
                brightest = glow;
                brightestAlong = along;
            }
        }
        return brightestAlong;
    }
}
