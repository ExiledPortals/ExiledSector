package exiledsector.charts.atlas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerritoryFactionsTest {

    @AfterEach
    void restoreNeutralFactions() {
        TerritoryExclusions.useForTests(Set.of());
    }

    private static AtlasRegion region(String regionId, Map<String, Float> factionWeights) {
        return new AtlasRegion(regionId, 5f, 0, "", "", Map.of(), factionWeights, Map.of());
    }

    private static AtlasRules rules(List<AtlasRegion> regions) {
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 5f, "hegemony", 3f, "independent", 2f, "derelict", 1f),
                AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER, List.of(), List.of());
    }

    @Test
    void aTerritoryIdNamesItsFactionAndCommandCentre() {
        AtlasCell centre = new AtlasCell(-3, 9);
        String territoryId = TerritoryFactions.territoryId("tritachyon", centre);

        assertEquals("tritachyon", TerritoryFactions.factionOfTerritory(territoryId));
        assertNotEquals(territoryId, TerritoryFactions.territoryId("tritachyon", new AtlasCell(9, -3)));
        assertNull(TerritoryFactions.factionOfTerritory(null));
    }

    @Test
    void independentsMercenariesThePlayerAndNeutralFactionsHoldNoTerritory() {
        TerritoryExclusions.useForTests(Set.of("derelict"));

        assertTrue(TerritoryFactions.canHoldTerritory("pirates"));
        assertFalse(TerritoryFactions.canHoldTerritory("independent"));
        assertFalse(TerritoryFactions.canHoldTerritory("mercenary"));
        assertFalse(TerritoryFactions.canHoldTerritory("player"));
        assertFalse(TerritoryFactions.canHoldTerritory("derelict"));
        assertFalse(TerritoryFactions.canHoldTerritory(null));
    }

    @Test
    void theBoardDrawsOnEveryTerritoryFactionOfTheNormalPoolAndTheRegionPools() {
        TerritoryExclusions.useForTests(Set.of("derelict"));
        AtlasRules rules = rules(List.of(region(AtlasRegion.NORMAL, Map.of()), region("badlands", Map.of("luddic_path", 1f, "derelict", 1f))));

        assertEquals(Set.of("pirates", "hegemony", "luddic_path"), TerritoryFactions.boardFactions(rules).keySet());
        assertEquals(5f, TerritoryFactions.boardFactions(rules).get("pirates"));
    }

    @Test
    void unclaimedSystemsAreIndependentOrMercenaryUnlessTheRegionHasOnlyNeutralFactions() {
        TerritoryExclusions.useForTests(Set.of("derelict"));
        AtlasRegion normal = region(AtlasRegion.NORMAL, Map.of());
        AtlasRegion graveyard = region("graveyard", Map.of("derelict", 1f));
        AtlasRules rules = rules(List.of(normal, graveyard));

        for (AtlasCell cell : AtlasCell.ORIGIN.around(4)) {
            assertTrue(Set.of("independent", "mercenary").contains(TerritoryFactions.unclaimedFaction(7L, rules, normal, cell)));
            assertEquals("derelict", TerritoryFactions.unclaimedFaction(7L, rules, graveyard, cell));
        }
        assertEquals(Map.of(), TerritoryFactions.territoryWeights(rules, graveyard));
    }

    @Test
    void anOutsideOwnerInARegionWithItsOwnTerritoryFactionsFacesTheirHoldouts() {
        AtlasRegion badlands = region("pirate_badlands", Map.of("pirates", 5f, "independent", 1f, "mercenary", 1f));
        AtlasCell cell = new AtlasCell(4, -2);

        assertEquals("pirates", TerritoryFactions.holdoutFaction(7L, badlands, cell, "luddic_church"));
        assertNull(TerritoryFactions.holdoutFaction(7L, badlands, cell, "pirates"));
        assertNull(TerritoryFactions.holdoutFaction(7L, badlands, cell, null));
    }

    @Test
    void theNormalRegionAndRegionsWithoutTerritoryFactionsHaveNoHoldouts() {
        TerritoryExclusions.useForTests(Set.of("remnant"));
        AtlasCell cell = new AtlasCell(4, -2);

        assertNull(TerritoryFactions.holdoutFaction(7L, region(AtlasRegion.NORMAL, Map.of("pirates", 1f, "hegemony", 1f)), cell, "tritachyon"));
        assertNull(TerritoryFactions.holdoutFaction(7L, region("storm_belt", Map.of()), cell, "tritachyon"));
        assertNull(TerritoryFactions.holdoutFaction(7L, region("remnant_expanse", Map.of("remnant", 1f)), cell, "tritachyon"));
    }
}
