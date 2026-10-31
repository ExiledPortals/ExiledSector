package exiledsector.items;

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
}
