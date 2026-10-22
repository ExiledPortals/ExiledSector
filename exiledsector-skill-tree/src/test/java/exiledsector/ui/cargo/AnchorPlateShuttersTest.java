package exiledsector.ui.cargo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorPlateShuttersTest {

    private static final float START = 30f;
    private static final float CLOSED_GAP = 0.6f;

    @Test
    void onlyTheTaniaAnchorUsesPlateShutters() {
        assertEquals(AnchorLook.Style.PLATE_SHUTTERS, new AnchorLook("navarchy_of_tania").style());
        assertEquals(AnchorLook.Style.LOCK_ON, new AnchorLook("hegemony").style());
        assertEquals(AnchorLook.Style.LOCK_ON, new AnchorLook(null).style());
    }

    @Test
    void thePlatesSlideShutStayClosedUntilTheApertureOpensThenPart() {
        assertEquals(START, AnchorIconFx.plateGap(0f, START), 1e-4f);
        assertEquals(CLOSED_GAP, AnchorIconFx.plateGap(0.22f, START), 1e-4f);
        assertEquals(CLOSED_GAP, AnchorIconFx.plateGap(0.29f, START), 1e-4f);
        assertTrue(AnchorIconFx.plateGap(0.37f, START) > CLOSED_GAP);
        assertEquals(START, AnchorIconFx.plateGap(0.5f, START), 1e-4f);
    }

    @Test
    void thePlatesFadeOutBeforeTheApertureClosesAndTheSeamGlowsOnlyWhileShut() {
        assertEquals(0f, AnchorIconFx.plateAlpha(0f));
        assertTrue(AnchorIconFx.plateAlpha(0.2f) > 0.99f);
        assertEquals(0f, AnchorIconFx.plateAlpha(0.5f));
        assertEquals(0f, AnchorIconFx.seamGlow(0.1f));
        assertEquals(1f, AnchorIconFx.seamGlow(0.23f));
        assertEquals(0.25f, AnchorIconFx.seamGlow(0.25f), 1e-4f);
        assertEquals(0f, AnchorIconFx.seamGlow(0.35f));
    }
}
