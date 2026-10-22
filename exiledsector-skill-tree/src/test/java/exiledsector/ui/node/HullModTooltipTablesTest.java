package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.ui.TooltipTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullModTooltipTablesTest {

    private static WeaponSlotAPI slot(WeaponType type, WeaponSize size) {
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.getWeaponType()).thenReturn(type);
        when(slot.getSlotSize()).thenReturn(size);
        return slot;
    }

    private static ShipHullSpecAPI hull(HullSize size, WeaponSlotAPI... slots) {
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        List<WeaponSlotAPI> slotList = new ArrayList<>(List.of(slots));
        when(hull.getHullSize()).thenReturn(size);
        when(hull.getAllWeaponSlotsCopy()).thenReturn(slotList);
        return hull;
    }

    private static SkillType passthrough(String hullModId) {
        return new SkillType.Builder(hullModId, hullModId, "", SkillTier.KEYSTONE).vanillaHullModId(hullModId).build();
    }

    @Test
    void typesWithoutATableHullModGetNoTables() {
        ShipHullSpecAPI hull = hull(HullSize.FRIGATE);

        assertTrue(HullModTooltipTables.forType(passthrough("heavyarmor"), hull).isEmpty());
        assertTrue(HullModTooltipTables.forType(null, hull).isEmpty());
    }

    @Test
    void rangefinderHighlightsTheLargeRowWhenTheLargestBallisticSlotIsLarge() {
        ShipHullSpecAPI hull = hull(HullSize.CRUISER, slot(WeaponType.BALLISTIC, WeaponSize.MEDIUM),
                slot(WeaponType.BALLISTIC, WeaponSize.LARGE), slot(WeaponType.ENERGY, WeaponSize.LARGE));

        List<TooltipTable> tables = HullModTooltipTables.forType(passthrough("ballistic_rangefinder"), hull);

        assertEquals(2, tables.size());
        TooltipTable ballistic = tables.get(0);
        assertEquals("Ballistic weapon range", ballistic.heading());
        assertEquals(List.of("Largest b. slot", "Small wpn", "Medium wpn", "Range cap"), ballistic.headers());
        assertEquals(List.of("Small / Medium", "+100", "---", "800"), ballistic.rows().get(0).cells());
        assertEquals(List.of("Large", "+200", "+100", "900"), ballistic.rows().get(1).cells());
        assertFalse(ballistic.rows().get(0).highlighted());
        assertTrue(ballistic.rows().get(1).highlighted());

        TooltipTable hybrid = tables.get(1);
        assertEquals(List.of("Small / Medium", "+200", "+100", "+100", "800"), hybrid.rows().get(0).cells());
        assertEquals(List.of("Large", "+400", "+200", "+100", "900"), hybrid.rows().get(1).cells());
        assertTrue(hybrid.rows().get(1).highlighted());
    }

    @Test
    void rangefinderIgnoresDecorativeSlotsAndHighlightsNothingWithoutBallisticSlots() {
        WeaponSlotAPI decorative = slot(WeaponType.BALLISTIC, WeaponSize.LARGE);
        when(decorative.isDecorative()).thenReturn(true);

        assertNull(HullModTooltipTables.largestBallisticSlot(hull(HullSize.FRIGATE, decorative)));
        for (TooltipTable table : HullModTooltipTables.rangefinderTables(null)) {
            assertFalse(table.rows().get(0).highlighted());
            assertFalse(table.rows().get(1).highlighted());
        }
    }

    @Test
    void autoloaderListsOnlyThisHullSizesRowsAndHighlightsTheOneMatchingItsSmallMissileSlots() {
        ShipHullSpecAPI hull = hull(HullSize.CRUISER, slot(WeaponType.MISSILE, WeaponSize.SMALL),
                slot(WeaponType.MISSILE, WeaponSize.SMALL), slot(WeaponType.MISSILE, WeaponSize.SMALL),
                slot(WeaponType.MISSILE, WeaponSize.MEDIUM));

        List<TooltipTable> tables = HullModTooltipTables.forType(passthrough("missile_autoloader"), hull);

        TooltipTable table = tables.get(0);
        assertEquals("Reload capacity", table.heading());
        assertEquals(List.of("Ship size", "Small missiles", "Reload capacity"), table.headers());
        assertEquals(3, table.rows().size());
        assertEquals(List.of("Cruiser", "4+", "8"), table.rows().get(0).cells());
        assertEquals(List.of("Cruiser", "3", "12"), table.rows().get(1).cells());
        assertEquals(List.of("Cruiser", "1-2", "15"), table.rows().get(2).cells());
        assertTrue(table.rows().get(1).highlighted());
        assertFalse(table.rows().get(0).highlighted());
        assertFalse(table.rows().get(2).highlighted());
    }
}
