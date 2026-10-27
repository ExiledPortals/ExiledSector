package exiledsector.charts.raid;

import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import exiledlib.i18n.Style;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartRaidTest {

    @BeforeAll
    static void loadDangerLevels() {
        RaidDangerLevels.load();
    }

    @Test
    void raidEffectivenessIsVanillasStrengthShareRoundedToAPercent() {
        assertEquals(0.75f, ChartRaid.raidEffectiveness(300f, 100f), 0.0001f);
        assertEquals(0.33f, ChartRaid.raidEffectiveness(100f, 200f), 0.0001f);
        assertEquals(0f, ChartRaid.raidEffectiveness(0f, 0f), 0.0001f);
    }

    @Test
    void marineTokensFollowEffectivenessBetweenVanillasLimits() {
        assertEquals(8, ChartRaid.marineTokens(0.75f));
        assertEquals(1, ChartRaid.marineTokens(0.01f));
        assertEquals(10, ChartRaid.marineTokens(1f));
    }

    @Test
    void effectivenessLeftOverAfterWholeTokensRaisesTheTake() {
        assertEquals(1f + 0.05f / 0.35f, ChartRaid.lootMult(0.35f), 0.0001f);
        assertEquals(1f, ChartRaid.lootMult(0.3f), 0.0001f);
        assertEquals(1f, ChartRaid.lootMult(0.05f), 0.0001f);
    }

    @Test
    void marineLossesShrinkWithStrongRaidsAndReservesAndGrowWithWeakOnes() {
        assertEquals(0.525f, ChartRaid.effectivenessLossMult(0.75f), 0.0001f);
        assertEquals(1.5f, ChartRaid.effectivenessLossMult(0.05f), 0.0001f);
        assertEquals(1f, ChartRaid.effectivenessLossMult(0.3f), 0.0001f);
        assertEquals(0.65f, ChartRaid.reserveLossMult(10, 3f), 0.0001f);
        assertEquals(1f, ChartRaid.reserveLossMult(4, 10f), 0.0001f);
        assertEquals(0.5f, ChartRaid.reserveLossMult(20, 0f), 0.0001f);
    }

    @Test
    void projectedLossesNameVanillasDangerBands() {
        assertEquals(RaidDangerLevel.NONE, ChartRaid.lossesLevel(0f, true));
        assertEquals(RaidDangerLevel.MINIMAL, ChartRaid.lossesLevel(0.005f, true));
        assertEquals(RaidDangerLevel.NONE, ChartRaid.lossesLevel(0.005f, false));
        assertEquals(RaidDangerLevel.LOW, ChartRaid.lossesLevel(0.05f, false));
        assertEquals(RaidDangerLevel.EXTREME, ChartRaid.lossesLevel(0.5f, false));
        assertEquals(Style.GOOD, ChartRaid.lossesStyle(RaidDangerLevel.LOW));
        assertEquals(Style.BAD, ChartRaid.lossesStyle(RaidDangerLevel.HIGH));
    }

    @Test
    void randomisedLossesNeverExceedTheMarinesAboard() {
        for (long seed = 0; seed < 50; seed++) {
            int losses = ChartRaid.randomizedLosses(40f, 30, new Random(seed));
            assertTrue(losses >= 0 && losses <= 30, "got " + losses);
        }
        assertEquals(0, ChartRaid.randomizedLosses(0f, 30, new Random(1L)));
    }

    @Test
    void onlyTheRaidsOwnOptionsAreHandled() {
        assertTrue(ChartRaid.handles(ChartRaid.RAID_CONFIRM));
        assertFalse(ChartRaid.handles("mktRaidConfirm"));
        assertFalse(ChartRaid.handles(null));
    }
}
