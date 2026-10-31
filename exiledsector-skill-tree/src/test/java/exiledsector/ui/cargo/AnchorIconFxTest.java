package exiledsector.ui.cargo;

import exiledsector.skills.skilleffect.render.Fade;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorIconFxTest {

    @Test
    void theCycleWrapsEverySixSecondsAndStaysInTheUnitRange() {
        assertEquals(0.5f, Fade.cycle(3f, AnchorIconFx.CYCLE_SECONDS), 1e-5f);
        assertEquals(Fade.cycle(1f, AnchorIconFx.CYCLE_SECONDS), Fade.cycle(1f + AnchorIconFx.CYCLE_SECONDS, AnchorIconFx.CYCLE_SECONDS), 1e-4f);
        float negative = Fade.cycle(-1f, AnchorIconFx.CYCLE_SECONDS);
        assertTrue(negative >= 0f && negative < 1f);
    }

    @Test
    void theChevronsConvergeBlinkTwiceAndFadeBeforeTheApertureCloses() {
        assertEquals(0f, AnchorIconFx.lockAlpha(0f));
        assertEquals(1f, AnchorIconFx.lockTravel(0.22f), 1e-5f);
        assertTrue(AnchorIconFx.lockAlpha(0.15f) > 0.99f);
        assertTrue(AnchorIconFx.lockAlpha(0.235f) > 0.99f, "lit on the first blink step");
        assertEquals(0.25f, AnchorIconFx.lockAlpha(0.26f), 1e-4f, "dimmed on the second");
        assertTrue(AnchorIconFx.lockAlpha(0.285f) > 0.99f);
        assertEquals(0f, AnchorIconFx.lockAlpha(0.5f));
    }

    @Test
    void theApertureOpensAfterTheLockAndClosesBeforeTheCycleEnds() {
        assertEquals(0f, AnchorIconFx.openness(0.2f));
        assertEquals(1f, AnchorIconFx.openness(0.5f), 1e-5f);
        assertEquals(0f, AnchorIconFx.openness(0.9f));
        for (float cycle = 0f; cycle < 1f; cycle += 0.005f) {
            for (int ring = 0; ring < 3; ring++) {
                float progress = AnchorIconFx.infallProgress(cycle, ring);
                boolean visible = progress > 0f && progress < 1f && Math.sin(progress * Math.PI) > 0.1;
                if (visible) {
                    assertTrue(AnchorIconFx.openness(cycle) > 0f, "ring " + ring + " falls only into an open aperture, at " + cycle);
                }
            }
        }
    }

    @Test
    void theLightsReachTheCrestJustBeforeTheNextLockAndRestWhenIdle() {
        float arrival = 0.9957f;
        assertTrue(AnchorIconFx.badgeFlash(AnchorIconFx.wave(arrival)) > 0.99f);
        assertEquals(0f, AnchorIconFx.lockAlpha(arrival));
        assertTrue(AnchorIconFx.badgeFlash(AnchorIconFx.wave(0.5f)) < 0.01f);
        for (float distance = 0f; distance <= 2f; distance += 0.1f) {
            assertEquals(0f, IconFxKit.lightLevel(distance, IconFxKit.IDLE_WAVE), 1e-6f);
        }
        assertEquals(1f, IconFxKit.lightLevel(0.7f, 0.7f), 1e-6f);
    }

    @Test
    void bothLightPathsRunFromTheTopLeftCornerToTheCrestCorner() {
        for (int path = 0; path < 2; path++) {
            assertArrayEquals(new float[]{-IconFxKit.FRAME_HALF, -IconFxKit.FRAME_HALF}, IconFxKit.framePoint(path, 0f), 1e-4f);
            assertArrayEquals(new float[]{IconFxKit.FRAME_HALF, IconFxKit.FRAME_HALF}, IconFxKit.framePoint(path, 1.9999f), 1e-2f);
        }
        assertArrayEquals(new float[]{IconFxKit.FRAME_HALF, -IconFxKit.FRAME_HALF}, IconFxKit.framePoint(0, 1f), 1e-4f);
        assertArrayEquals(new float[]{-IconFxKit.FRAME_HALF, IconFxKit.FRAME_HALF}, IconFxKit.framePoint(1, 1f), 1e-4f);
    }

    @Test
    void theTunnelFitsInsideTheIconBoxAndFadesAtBothEnds() {
        for (float inset : new float[]{CargoIconFx.ICON_INSET, 1f}) {
            float reach = AnchorIconFx.tunnelReach(inset);
            float gemX = 40f + (36.5f - 40f) * inset;
            float gemFromTop = 40f + (31f - 40f) * inset;
            assertTrue(gemX - reach >= 0f && gemX + reach <= 80f);
            assertTrue(gemFromTop - reach >= 0f && gemFromTop + reach <= 80f);
            float chevronArmReach = 3.2f * (float) Math.sqrt(2.0);
            assertTrue(AnchorIconFx.lockStartDistance(reach) + chevronArmReach <= reach, "chevrons start inside the box");
        }
        assertEquals(0f, AnchorIconFx.tunnelFade(0.06f));
        assertEquals(0f, AnchorIconFx.tunnelFade(1f));
        assertTrue(AnchorIconFx.tunnelFade(0.45f) > 0.99f);
    }

    @Test
    void lighteningMovesEachChannelTowardsWhite() {
        assertEquals(new Color(250, 203, 143), CargoIconFx.lighten(new Color(245, 150, 30), 0.5f));
        assertEquals(Color.WHITE, CargoIconFx.lighten(Color.BLACK, 1f));
        assertNull(AnchorIconFx.crestPath(null));
    }
}
