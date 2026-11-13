package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.DenseNebula;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.run.ChartRuns;
import exiledsector.loot.BattleLoot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarChartLootSourceTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartAnomalies.resetForTests();
    }

    private static BattleLoot battle(boolean won) {
        BattleLoot battle = new BattleLoot(new Random(1L), new Random(2L));
        battle.setPlayerWon(won);
        return battle;
    }

    @Test
    void onlyAFoughtBattleThePlayerWonCountsForTheRunsAnomalies() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        BattleLoot unfought = battle(true);
        unfought.markUnfought();

        StarChartLootSource.recordWin(battle(false), system);
        StarChartLootSource.recordWin(unfought, system);
        assertEquals(0, DenseNebula.dust(ChartRuns.currentOrNull()));

        StarChartLootSource.recordWin(battle(true), system);
        assertEquals(1, DenseNebula.dust(ChartRuns.currentOrNull()));
    }
}
