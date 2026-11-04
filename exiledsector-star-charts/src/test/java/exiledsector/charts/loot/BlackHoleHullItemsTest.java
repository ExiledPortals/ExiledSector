package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemKind;
import exiledsector.loot.LootMultipliers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlackHoleHullItemsTest {

    private static PlanetAPI star(boolean blackHole) {
        PlanetAPI star = mock(PlanetAPI.class);
        when(star.isStar()).thenReturn(true);
        when(star.isBlackHole()).thenReturn(blackHole);
        return star;
    }

    private static StarSystemAPI system(boolean chart, PlanetAPI... planets) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(chart);
        when(system.getPlanets()).thenReturn(List.of(planets));
        return system;
    }

    @Test
    void anyBlackHoleAmongTheSystemsStarsCounts() {
        assertTrue(BlackHoleHullItems.hasBlackHole(system(true, star(false), star(true))));
        assertFalse(BlackHoleHullItems.hasBlackHole(system(true, star(false), mock(PlanetAPI.class))));
    }

    @Test
    void theBonusAddsTheSameExtraShareToEveryHullItemPartAndLeavesTheRestAlone() {
        ItemChances chances = new ItemChances(1.5f, Map.of(ItemKind.BRIDGE, 0.2f, ItemKind.SUBROUTINE, 0.3f), 1.1f);

        ItemChances boosted = BlackHoleHullItems.withExtraHullItemShare(chances, 0.1f);

        assertEquals(1.5f, boosted.general());
        assertEquals(1.1f, boosted.rarity());
        assertEquals(0.3f, boosted.targeted(ItemKind.BRIDGE), 0.0001f);
        assertEquals(0.3f, boosted.targeted(ItemKind.SUBROUTINE), 0.0001f);
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            assertTrue(boosted.targeted(kind) >= 0.1f - 0.0001f, kind.id());
        }
        assertSame(chances, BlackHoleHullItems.withExtraHullItemShare(chances, 0f));
    }

    @Test
    void systemsOutsideStarChartsOrWithoutABlackHoleGetNoBonus() {
        LootMultipliers multipliers = LootMultipliers.of(1.5f);

        assertSame(multipliers, BlackHoleHullItems.withBonus(multipliers, system(false, star(true))));
        assertSame(multipliers, BlackHoleHullItems.withBonus(multipliers, system(true, star(false))));
        assertSame(multipliers, BlackHoleHullItems.withBonus(multipliers, mock(LocationAPI.class)));
        assertSame(multipliers, BlackHoleHullItems.withBonus(multipliers, null));
    }
}
