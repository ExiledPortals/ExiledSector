package exiledsector.charts;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.expansion.SpatialBreach;
import exiledsector.loot.LootModifiers;
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
        LootModifiers modifiers = StarChartLootSource.INSTANCE.modifiersFor(site(FakeChartContentEngine.memory()));

        assertEquals(1f, modifiers.itemChances().general());
        assertEquals(1f, modifiers.itemChances().rarity());
    }

    @Test
    void breachRarityAndCoreQuantityRaiseTheSitesItemChances() {
        MemoryAPI memory = FakeChartContentEngine.memory();
        memory.set(SpatialBreach.RARITY_KEY, 50f);
        memory.set(SpatialBreach.ITEM_MULT_KEY, 2f);

        LootModifiers modifiers = StarChartLootSource.INSTANCE.modifiersFor(site(memory));

        assertEquals(2f, modifiers.itemChances().general(), 0.001f);
        assertEquals(1.5f, modifiers.itemChances().rarity(), 0.001f);
    }

    @Test
    void noSiteHasNoModifiers() {
        assertSame(null, StarChartLootSource.INSTANCE.modifiersFor(null));
    }
}
