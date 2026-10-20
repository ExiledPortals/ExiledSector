package exiledsector.ui.cargo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctrineIconFxTest {

    @Test
    void eachFactionHasItsOwnScreenAndUnknownFactionsGetThePlainReadout() {
        assertEquals(DoctrineScreen.SEAL, DoctrineScreen.forFaction("hegemony"));
        assertEquals(DoctrineScreen.DECRYPT, DoctrineScreen.forFaction("tritachyon"));
        assertEquals(DoctrineScreen.MANUSCRIPT, DoctrineScreen.forFaction("luddic_church"));
        assertEquals(DoctrineScreen.DRILL, DoctrineScreen.forFaction("lions_guard"));
        assertEquals(DoctrineScreen.GLITCH, DoctrineScreen.forFaction("pirates"));
        assertEquals(DoctrineScreen.COMMITTEE, DoctrineScreen.forFaction("persean"));
        assertEquals(DoctrineScreen.REDACTED, DoctrineScreen.forFaction("remnant"));
        assertEquals(DoctrineScreen.READOUT, DoctrineScreen.forFaction("sindrian_diktat"));
        assertEquals(DoctrineScreen.READOUT, DoctrineScreen.forFaction(null));
    }

    @Test
    void theFlashPeaksOnTheKeyMomentAndWrapsAcrossTheCycleEnd() {
        assertEquals(1f, DoctrineIconFx.flash(DoctrineIconFx.SEAL_KEY, DoctrineIconFx.SEAL_KEY), 1e-5f);
        assertTrue(DoctrineIconFx.flash(0.2f, DoctrineIconFx.SEAL_KEY) < 0.01f);
        assertEquals(DoctrineIconFx.flash(0.99f, 0.01f), DoctrineIconFx.flash(0.03f, 0.01f), 1e-4f);
        assertEquals(DoctrineIconFx.cycleOf(1f), DoctrineIconFx.cycleOf(1f + DoctrineIconFx.CYCLE_SECONDS), 1e-4f);
    }

    @Test
    void theFrameLightsReachTheFarCornerOnTheKeyMoment() {
        for (DoctrineScreen screen : DoctrineScreen.values()) {
            float key = DoctrineIconFx.keyMoment(screen);
            assertEquals(2f, DoctrineIconFx.wave(key, key), 1e-4f, screen.name());
            assertTrue(DoctrineIconFx.wave(key - 0.05f, key) < 2f);
        }
    }

    @Test
    void theHegemonySealFadesOutThenSlamsDownFromLargeToNormalOnTheKey() {
        float key = DoctrineIconFx.SEAL_KEY;
        assertEquals(1f, DoctrineIconFx.stampAlpha(0.2f));
        assertEquals(1f, DoctrineIconFx.stampScale(0.2f));
        assertTrue(DoctrineIconFx.stampAlpha(key - DoctrineIconFx.STAMP_DROP - 0.01f) < 0.25f);
        assertEquals(DoctrineIconFx.STAMP_SCALE, DoctrineIconFx.stampScale(key - DoctrineIconFx.STAMP_DROP), 1e-4f);
        assertTrue(DoctrineIconFx.stampScale(key - 0.01f) < DoctrineIconFx.stampScale(key - 0.03f));
        assertEquals(1f, DoctrineIconFx.stampScale(key));
        assertEquals(1f, DoctrineIconFx.stampAlpha(key));
    }

    @Test
    void triTachyonRowsClearOverThreePassesAndAllAreReadableByTheKey() {
        assertEquals(0, DoctrineIconFx.decryptLevel(0.05f, 0f));
        assertEquals(1, DoctrineIconFx.decryptLevel(DoctrineIconFx.PASS_START + 0.01f, 0f));
        assertEquals(0, DoctrineIconFx.decryptLevel(DoctrineIconFx.PASS_START + 0.01f, 1f), "the first sweep has not reached the bottom row yet");
        assertEquals(3, DoctrineIconFx.decryptLevel(DoctrineIconFx.DECRYPT_KEY, 1f));
        assertEquals(0, DoctrineIconFx.decryptLevel(DoctrineIconFx.DECRYPT_RESET, 0f));
        int[] needed = new int[4];
        for (int row = 0; row < 200; row++) {
            int need = DoctrineIconFx.rowsNeeded(17, row);
            assertTrue(need >= 1 && need <= 3);
            needed[need]++;
        }
        assertTrue(needed[1] > 0 && needed[2] > 0 && needed[3] > 0);
    }

    @Test
    void theLuddicPageIsCopiedRowByRowThenHeldThenCleared() {
        assertEquals(0f, DoctrineIconFx.writeProgress(0f));
        float midway = DoctrineIconFx.writeProgress((DoctrineIconFx.WRITE_START + DoctrineIconFx.WRITE_END) / 2f);
        assertEquals(DoctrineIconFx.ROWS / 2f, midway, 1e-3f);
        assertEquals(DoctrineIconFx.ROWS, DoctrineIconFx.writeProgress(DoctrineIconFx.MANUSCRIPT_KEY), 1e-5f);
        assertEquals(1f, DoctrineIconFx.pageAlpha(DoctrineIconFx.MANUSCRIPT_KEY));
        assertEquals(0f, DoctrineIconFx.pageAlpha(0.995f));
    }

    @Test
    void theLionsGuardRowsHoldStillBetweenBeatsAndStepOneRowPerBeat() {
        float beat = DoctrineIconFx.BEAT_SECONDS;
        assertEquals(1f, DoctrineIconFx.drillOffset(beat), 1e-5f);
        assertEquals(2f, DoctrineIconFx.drillOffset(beat * 1.5f), 1e-5f);
        assertEquals(DoctrineIconFx.drillOffset(beat * 1.5f), DoctrineIconFx.drillOffset(beat * 1.9f), 1e-5f);
        assertEquals(3f, DoctrineIconFx.drillOffset(beat * 2.5f), 1e-5f);
        assertEquals(DoctrineIconFx.DRILL_KEY, (DoctrineIconFx.BEATS - 1f) / DoctrineIconFx.BEATS);
    }

    @Test
    void thePirateScreenDropsOutAndComesBackOnTheKey() {
        assertTrue(DoctrineIconFx.dropped(DoctrineIconFx.DROPOUT_START));
        assertFalse(DoctrineIconFx.dropped(DoctrineIconFx.GLITCH_KEY));
        assertFalse(DoctrineIconFx.dropped(0.3f));
    }

    @Test
    void theLeagueFinishesItsLastRewriteOnTheKeyAndEditsDifferentLinesEachSession() {
        float lastEnd = DoctrineIconFx.editStart(DoctrineIconFx.EDITS - 1) + DoctrineIconFx.STRIKE_SPAN + DoctrineIconFx.ERASE_SPAN
                + DoctrineIconFx.REWRITE_SPAN;
        assertEquals(DoctrineIconFx.COMMITTEE_KEY, lastEnd, 1e-5f);
        assertTrue(DoctrineIconFx.COMMITTEE_KEY < 0.95f, "the page holds before it adjourns");
        int line = DoctrineIconFx.editedLine(5, 0, 0);
        assertTrue(line >= 0 && line < DoctrineIconFx.ROWS * 2);
        boolean varies = false;
        for (int session = 1; session < 20; session++) {
            varies |= DoctrineIconFx.editedLine(5, session, 0) != line;
        }
        assertTrue(varies);
        assertEquals(0f, DoctrineIconFx.committeeAlpha(0f));
        assertEquals(1f, DoctrineIconFx.committeeAlpha(0.5f));
    }

    @Test
    void theRedactedBarsAllGoDarkJustBeforeTheKey() {
        assertTrue(DoctrineIconFx.blackedOut(DoctrineIconFx.REDACTED_KEY - 0.01f));
        assertFalse(DoctrineIconFx.blackedOut(DoctrineIconFx.REDACTED_KEY));
    }
}
