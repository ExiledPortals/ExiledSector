package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.loot.LootMultipliers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarChartSiteModifiersTest {

    private static SectorEntityToken site(MemoryAPI memory) {
        SectorEntityToken site = mock(SectorEntityToken.class);
        when(site.getContainingLocation()).thenReturn(mock(LocationAPI.class));
        when(site.getMemoryWithoutUpdate()).thenReturn(memory);
        return site;
    }

    @Test
    void anOrdinarySiteGetsTheLocationsModifiers() {
        LootMultipliers multipliers = StarChartLootSource.INSTANCE.lootMultipliersFor(site(FakeChartContentEngine.memory()));

        assertEquals(1f, multipliers.chances().general());
        assertEquals(1f, multipliers.chances().rarity());
    }

    @Test
    void breachCacheQuantityRaisesOnlyTheSitesItemQuantity() {
        MemoryAPI memory = FakeChartContentEngine.memory();
        memory.set(SpatialBreach.ITEM_MULT_KEY, 2f);

        LootMultipliers multipliers = StarChartLootSource.INSTANCE.lootMultipliersFor(site(memory));

        assertEquals(2f, multipliers.chances().general(), 0.001f);
        assertEquals(1f, multipliers.chances().rarity(), 0.001f);
    }

    @Test
    void noSiteHasNoModifiers() {
        assertSame(null, StarChartLootSource.INSTANCE.lootMultipliersFor(null));
    }
}
