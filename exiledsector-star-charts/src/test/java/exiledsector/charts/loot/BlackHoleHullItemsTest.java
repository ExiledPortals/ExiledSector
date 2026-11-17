package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.items.drop.ItemChances;
import exiledsector.loot.DropType;
import exiledsector.loot.LootMultipliers;
import exiledsector.skills.skilleffect.logistics.BlackHoleSalvage;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    void theBonusAddsAtTheHullItemNodeSoEverySectionGetsItAndTheRestDoesNot() {
        ItemChances chances = new ItemChances(Map.of(DropType.ITEM, 50f, DropType.BRIDGE, 20f), Map.of(DropType.ITEM, 10f), 1f);
        LootMultipliers multipliers = new LootMultipliers(chances, 1f);
        StarSystemAPI blackHoleChart = system(true, star(true));

        try (MockedStatic<BlackHoleSalvage> salvage = Mockito.mockStatic(BlackHoleSalvage.class)) {
            salvage.when(() -> BlackHoleSalvage.hullItemPercent(any())).thenReturn(10f);
            ItemChances boosted = BlackHoleHullItems.withBonus(multipliers, blackHoleChart).chances();

            assertEquals(1.6f, boosted.quantity(DropType.HULL_ITEM), 0.0001f);
            assertEquals(1.8f, boosted.quantity(DropType.BRIDGE), 0.0001f);
            assertEquals(1.6f, boosted.quantity(DropType.FLIGHT_DECK), 0.0001f);
            assertEquals(1.5f, boosted.quantity(DropType.SUBROUTINE), 0.0001f);
            assertEquals(1.5f, boosted.quantity(DropType.STAR_CHART), 0.0001f);
            assertEquals(1.1f, boosted.rarity(DropType.BRIDGE), 0.0001f);

            salvage.when(() -> BlackHoleSalvage.hullItemPercent(any())).thenReturn(0f);
            assertSame(multipliers, BlackHoleHullItems.withBonus(multipliers, blackHoleChart));
        }
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
