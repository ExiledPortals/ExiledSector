package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.DenseNebula;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.LuckyRolls;
import exiledsector.items.PoolEntry;
import exiledsector.loot.BattleLoot;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class StarChartLootSourceTest {

    private static final String PLATE = "flare_plate";

    @BeforeEach
    void setUp() {
        ItemDefinitions.register(List.of(new ItemDefinition(PLATE, ItemKind.ARMOR_PLATING, PLATE, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 1f, 100f, 1f)),
                List.of(PoolEntry.of(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 1f, 100f, 1f)), "")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        ChartAnomalies.resetForTests();
    }

    private static BattleLoot battle(boolean won) {
        BattleLoot battle = new BattleLoot(new Random(1L), new Random(2L));
        battle.setPlayerWon(won);
        return battle;
    }

    private static ItemData dropped(long seed) {
        return ItemData.rolledWithModifierCount(ItemDefinitions.get(PLATE), seed, 2);
    }

    @Test
    void onlyAFoughtBattleThePlayerWonCountsForTheRunsAnomalies() {
        try (MockSector ignored = new MockSector()) {
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

    @Test
    void everyItemDroppedInAStellarFlaresSystemIsLucky() {
        try (MockSector ignored = new MockSector()) {
            StarSystemAPI system = LiveAnomalyRuns.systemWith(MagneticStorm.ANOMALY_ID, StellarFlares.ANOMALY_ID);
            int changed = 0;
            for (long seed = 0L; seed < 50L; seed++) {
                ItemData item = dropped(seed);
                ItemData lucky = LuckyRolls.withLuckyModifier(item);

                assertEquals(lucky, StarChartLootSource.INSTANCE.shapeDrop(item, system));
                assertEquals(lucky, StarChartLootSource.INSTANCE.shapeBattleDrop(item, system));
                changed += lucky.equals(item) ? 0 : 1;
            }
            assertTrue(changed > 0);
        }
    }

    @Test
    void itemsDroppedWithoutTheFlaresAreLeftAsTheyAre() {
        try (MockSector ignored = new MockSector()) {
            StarSystemAPI calm = LiveAnomalyRuns.systemWith(MagneticStorm.ANOMALY_ID);
            StarSystemAPI outside = mock(StarSystemAPI.class);
            for (long seed = 0L; seed < 20L; seed++) {
                ItemData item = dropped(seed);

                assertSame(item, StarChartLootSource.INSTANCE.shapeDrop(item, calm));
                assertSame(item, StarChartLootSource.INSTANCE.shapeDrop(item, outside));
            }
        }
    }
}
