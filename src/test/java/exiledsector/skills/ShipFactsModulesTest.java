package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.skills.tags.ShipProfile;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipFactsModulesTest {

    private static ShipHullSpecAPI hull(boolean withModuleSlot) {
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.isStationModule()).thenReturn(withModuleSlot);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getAllWeaponSlotsCopy()).thenReturn(List.of(slot));
        when(hullSpec.getHullSize()).thenReturn(HullSize.CRUISER);
        return hullSpec;
    }

    @Test
    void playerFactsSeeModulesFromTheHullsStationModuleSlots() {
        assertTrue(ShipFacts.of(hull(true), id -> false).hasModules());
        assertFalse(ShipFacts.of(hull(false), id -> false).hasModules());
    }

    @Test
    void npcFactsLookUpTheHullToSeeModules() {
        ShipProfile profile = new ShipProfile(HullSize.CRUISER, ShieldType.FRONT, 0, Set.of(), false, 500f, false);
        SettingsAPI settings = mock(SettingsAPI.class);
        ShipHullSpecAPI plated = hull(true);
        ShipHullSpecAPI plain = hull(false);
        when(settings.getHullSpec("tania_argentavis_main")).thenReturn(plated);
        when(settings.getHullSpec("eagle")).thenReturn(plain);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);

            assertTrue(ShipFacts.of(profile, id -> false, "tania_argentavis_main").hasModules());
            assertFalse(ShipFacts.of(profile, id -> false, "eagle").hasModules());
            assertFalse(ShipFacts.of(profile, id -> false, null).hasModules());
        }
    }
}
