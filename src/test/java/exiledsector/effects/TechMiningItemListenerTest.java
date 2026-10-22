package exiledsector.effects;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.TechMining;
import org.junit.jupiter.api.Test;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.charts.StarChartLootSource;
import exiledsector.charts.ChartDrops;
import exiledsector.items.ItemData;
import exiledsector.items.StarChartFixtures;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TechMiningItemListenerTest {

    private static final float DECAY = 0.95f;

    private static MemoryAPI memory(Integer months) {
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.contains(TechMiningItemListener.MONTHS_KEY)).thenReturn(months != null);
        when(memory.getInt(TechMiningItemListener.MONTHS_KEY)).thenReturn(months == null ? 0 : months);
        return memory;
    }

    private static TechMining vanillaAt(float mult) {
        TechMining techMining = mock(TechMining.class);
        when(techMining.getTechMiningMult()).thenReturn(mult);
        return techMining;
    }

    @Test
    void aFreshColonyStartsWithTheFirstFindEvenThoughVanillaDecayedJustBeforeUs() {
        assertEquals(0, TechMiningItemListener.monthsMined(memory(null), vanillaAt(DECAY), DECAY));
        assertEquals(0, TechMiningItemListener.monthsMined(memory(null), vanillaAt(1f), DECAY));
    }

    @Test
    void theCountedMonthsWinOnceRecorded() {
        assertEquals(7, TechMiningItemListener.monthsMined(memory(7), vanillaAt(1f), DECAY));
    }

    @Test
    void aColonyThatWasMiningBeforeTheUpdateSkipsTheFirstFindAndPicksUpVanillasDecay() {
        float afterTenFindsIncludingThisMonth = (float) Math.pow(DECAY, 10);

        assertEquals(9, TechMiningItemListener.monthsMined(memory(null), vanillaAt(afterTenFindsIncludingThisMonth), DECAY));
        assertEquals(0, TechMiningItemListener.monthsMined(memory(null), mock(Industry.class), DECAY));
    }

    @Test
    void aTechMiningFindCanTurnUpALowTierStarChartFromTheFirstFindOrLaterMonths() throws Exception {
        StarChartFixtures.registerShipped();
        ChartDrops.register(new JSONArray()
                .put(new JSONObject().put("source", "techmining_first_find").put("chance", "1").put("min_strength", "1")
                        .put("strength_per_tier", "3").put("max_tier", "5"))
                .put(new JSONObject().put("source", "techmining_monthly").put("chance", "0")));
        try {
            SectorAPI sector = mock(SectorAPI.class);
            when(sector.getSeedString()).thenReturn("seed");
            MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
            when(playerStats.getLevel()).thenReturn(30);
            when(sector.getPlayerStats()).thenReturn(playerStats);

            List<ItemData> firstFind = StarChartLootSource.techMiningCharts(sector, "colony", 0, 1f);
            assertEquals(1, firstFind.size());
            assertEquals(5, firstFind.get(0).preview().tier());
            assertEquals(firstFind.get(0).seed(), StarChartLootSource.techMiningCharts(sector, "colony", 0, 1f).get(0).seed());
            assertEquals(List.of(), StarChartLootSource.techMiningCharts(sector, "colony", 3, 1f));
        } finally {
            ChartDrops.clear();
            StarChartFixtures.clear();
        }
    }
}
