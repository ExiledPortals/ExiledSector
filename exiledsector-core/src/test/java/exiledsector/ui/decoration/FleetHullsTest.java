package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetHullsTest {

    @Test
    void fleetsAreLedByAWarshipAndEscortedByFrigatesAndDestroyers() {
        List<ShipHullSpecAPI> hulls = List.of(hull("wolf", HullSize.FRIGATE), hull("hammerhead", HullSize.DESTROYER),
                hull("eagle", HullSize.CRUISER), hull("onslaught", HullSize.CAPITAL_SHIP));
        Random random = new Random(11L);
        for (int fleet = 0; fleet < 200; fleet++) {
            List<ShipHullSpecAPI> picked = FleetHulls.pickHulls(hulls, random);

            assertTrue(picked.size() >= FleetHulls.MIN_SHIPS_PER_FLEET && picked.size() <= FleetHulls.MAX_SHIPS_PER_FLEET);
            assertNotEquals(HullSize.FRIGATE, picked.get(0).getHullSize());
            for (ShipHullSpecAPI escort : picked.subList(1, picked.size())) {
                assertTrue(escort.getHullSize() == HullSize.FRIGATE || escort.getHullSize() == HullSize.DESTROYER);
            }
        }
    }

    @Test
    void aFrigateOnlyFactionStillFormsFleets() {
        List<ShipHullSpecAPI> picked = FleetHulls.pickHulls(List.of(hull("wolf", HullSize.FRIGATE)), new Random(1L));

        assertTrue(picked.stream().allMatch(hull -> hull.getHullSize() == HullSize.FRIGATE));
    }

    @Test
    void onlyWarshipHullsRoam() {
        assertTrue(FleetHulls.roams(hull("wolf", HullSize.FRIGATE)));
        assertFalse(FleetHulls.roams(hull("talon", HullSize.FIGHTER)));
        ShipHullSpecAPI station = hull("station", HullSize.CAPITAL_SHIP);
        when(station.getHints()).thenReturn(EnumSet.of(ShipTypeHints.STATION));
        assertFalse(FleetHulls.roams(station));
        ShipHullSpecAPI freighter = hull("buffalo", HullSize.DESTROYER);
        when(freighter.isCivilianNonCarrier()).thenReturn(true);
        assertFalse(FleetHulls.roams(freighter));
    }

    private static ShipHullSpecAPI hull(String hullId, HullSize hullSize) {
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullId()).thenReturn(hullId);
        when(hull.getHullSize()).thenReturn(hullSize);
        when(hull.getSpriteName()).thenReturn("graphics/ships/" + hullId + ".png");
        when(hull.getHints()).thenReturn(EnumSet.noneOf(ShipTypeHints.class));
        return hull;
    }
}
