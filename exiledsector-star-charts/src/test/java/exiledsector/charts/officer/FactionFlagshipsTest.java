package exiledsector.charts.officer;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FactionFlagshipsTest {

    private static final int DRAWS = 4000;

    private static FactionFlagships.Candidate hull(String hullId, HullSize size, float deploymentPoints) {
        return new FactionFlagships.Candidate(hullId, size, deploymentPoints, List.of(hullId + "_Standard"));
    }

    private static final List<FactionFlagships.Candidate> FLEET = List.of(
            hull("wolf", HullSize.FRIGATE, 5f), hull("tempest", HullSize.FRIGATE, 8f), hull("lasher", HullSize.FRIGATE, 4f),
            hull("hammerhead", HullSize.DESTROYER, 10f), hull("medusa", HullSize.DESTROYER, 12f), hull("enforcer", HullSize.DESTROYER, 9f),
            hull("eagle", HullSize.CRUISER, 22f), hull("dominator", HullSize.CRUISER, 25f),
            hull("onslaught", HullSize.CAPITAL_SHIP, 40f), hull("paragon", HullSize.CAPITAL_SHIP, 60f));

    private static Map<HullSize, Integer> sizesDrawn(float rankShare) {
        Random random = new Random(7L);
        Map<HullSize, Integer> counts = new EnumMap<>(HullSize.class);
        for (int draw = 0; draw < DRAWS; draw++) {
            counts.merge(FactionFlagships.choose(FLEET, rankShare, random).hullSize(), 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void theLowestRankFliesMostlyCruisersAndCapitals() {
        Map<HullSize, Integer> counts = sizesDrawn(0f);
        int large = counts.getOrDefault(HullSize.CRUISER, 0) + counts.getOrDefault(HullSize.CAPITAL_SHIP, 0);
        int small = counts.getOrDefault(HullSize.FRIGATE, 0) + counts.getOrDefault(HullSize.DESTROYER, 0);
        assertTrue(large > DRAWS * 0.8f);
        assertTrue(small > 0);
    }

    @Test
    void theHighestRankNeverFliesAFrigateOrDestroyer() {
        Map<HullSize, Integer> counts = sizesDrawn(1f);
        assertEquals(0, counts.getOrDefault(HullSize.FRIGATE, 0) + counts.getOrDefault(HullSize.DESTROYER, 0));
        assertTrue(counts.get(HullSize.CAPITAL_SHIP) > counts.get(HullSize.CRUISER));
    }

    @Test
    void smallFlagshipsComeFromTheHighestDeploymentPointHalfOfTheirSize() {
        Random random = new Random(11L);
        List<FactionFlagships.Candidate> frigates = FLEET.stream().filter(candidate -> candidate.hullSize() == HullSize.FRIGATE).toList();
        for (int draw = 0; draw < 200; draw++) {
            String picked = FactionFlagships.choose(frigates, 0f, random).hullId();
            assertTrue(Set.of("tempest", "wolf").contains(picked), picked);
        }
    }

    @Test
    void aFactionWithOnlyFrigatesStillGetsAFlagship() {
        List<FactionFlagships.Candidate> frigates = List.of(hull("wolf", HullSize.FRIGATE, 5f));
        assertEquals("wolf", FactionFlagships.choose(frigates, 1f, new Random(3L)).hullId());
    }

    @Test
    void aFactionWithNoWarshipsGetsNone() {
        assertNull(FactionFlagships.choose(List.of(), 0.5f, new Random(3L)));
    }

    private static ShipHullSpecAPI spec(String designation, ShipTypeHints... hints) {
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getDesignation()).thenReturn(designation);
        EnumSet<ShipTypeHints> hintSet = EnumSet.noneOf(ShipTypeHints.class);
        hintSet.addAll(List.of(hints));
        when(hullSpec.getHints()).thenReturn(hintSet);
        return hullSpec;
    }

    @Test
    void freightersTankersAndTransportsAreLogistics() {
        assertTrue(FactionFlagships.isLogistics(spec("Freighter", ShipTypeHints.CIVILIAN, ShipTypeHints.FREIGHTER)));
        assertTrue(FactionFlagships.isLogistics(spec("Combat Freighter")));
        assertTrue(FactionFlagships.isLogistics(spec("Modified Superfreighter")));
        assertTrue(FactionFlagships.isLogistics(spec("Modified Tanker")));
        assertTrue(FactionFlagships.isLogistics(spec("Drone Tender", ShipTypeHints.CARRIER)));
        assertTrue(FactionFlagships.isLogistics(spec("Shuttle")));
        assertTrue(FactionFlagships.isLogistics(spec("Phase Transport", ShipTypeHints.CIVILIAN, ShipTypeHints.TRANSPORT)));
    }

    @Test
    void warshipsAreNotLogistics() {
        assertFalse(FactionFlagships.isLogistics(spec("Battleship", ShipTypeHints.COMBAT)));
        assertFalse(FactionFlagships.isLogistics(spec("Light Carrier", ShipTypeHints.CARRIER)));
        assertFalse(FactionFlagships.isLogistics(spec("Contender")));
        assertFalse(FactionFlagships.isLogistics(spec(null)));
    }
}
