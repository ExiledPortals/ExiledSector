package exiledsector.items.drop;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionFleetDropsTest {

    @Test
    void aGuaranteedSourceDropsFromAnyWonBattleAgainstItsFaction() {
        FactionFleetDrops.Source shrouded = FactionFleetDrops.Source.forVolume("shrouded", Set.of("dweller"), 0.1f, true);

        assertEquals(1f, FactionFleetDrops.chance(shrouded, Map.of("dweller", 5f)));
        assertEquals(0f, FactionFleetDrops.chance(shrouded, Map.of("hegemony", 500f)));
    }

    @Test
    void anOrdinarySourceScalesWithDeploymentPointsUpToTheCap() {
        FactionFleetDrops.Source threat = FactionFleetDrops.Source.forVolume("threat", Set.of("threat"), 0.1f);

        assertEquals(0.05f, FactionFleetDrops.chance(threat, Map.of("threat", 50f)), 1e-6f);
        assertEquals(FactionFleetDrops.MAX_CHANCE_PER_BATTLE, FactionFleetDrops.chance(threat, Map.of("threat", 5000f)));
    }

    @Test
    void shipsBorrowedIntoAnotherFactionsFleetCountForTheirOwnFactionPerDeploymentPoint() {
        FactionFleetDrops.Source threat = FactionFleetDrops.Source.forVolume("threat", Set.of("threat"), 0.1f);
        Map<String, Float> byShip = Map.of("threat", 50f, "exiledSector_dormant", 100f);
        Map<String, Float> byFleet = Map.of("exiledSector_dormant", 150f);

        assertEquals(0.05f, FactionFleetDrops.chance(threat, byShip, byFleet), 1e-6f);
        assertEquals(0f, FactionFleetDrops.chance(threat, byFleet, byFleet));
    }

    @Test
    void theShroudedGuaranteeNeedsARealDwellerFleetAndBorrowedShroudedShipsRollPerDeploymentPoint() {
        FactionFleetDrops.Source shrouded = FactionFleetDrops.Source.forVolume("shrouded", Set.of("dweller"), 0.15f, true);
        Map<String, Float> borrowed = Map.of("dweller", 50f, "exiledSector_dormant", 100f);

        assertEquals(0.075f, FactionFleetDrops.chance(shrouded, borrowed, Map.of("exiledSector_dormant", 150f)), 1e-6f);
        assertEquals(1f, FactionFleetDrops.chance(shrouded, Map.of("dweller", 5f), Map.of("dweller", 5f)));
        assertEquals(1f, FactionFleetDrops.chance(shrouded, Map.of("dweller", 55f, "remnant", 10f), Map.of("dweller", 5f, "exiledSector_dormant", 60f)));
        assertEquals(0f, FactionFleetDrops.chance(shrouded, Map.of("exiledSector_dormant", 150f), Map.of("dweller", 0f)));
        assertEquals(0f, FactionFleetDrops.chance(shrouded, Map.of("remnant", 50f), null));
    }
}
