package exiledsector.charts.atlas;

import exiledsector.items.Consumables;
import exiledsector.items.CraftingMaterials;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedeploymentCatalystUseTest {

    private static final long SEED = 0x2EDE9L;
    private static final List<AtlasRegion> REGIONS = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
    private static final Map<String, Float> THREE_FACTIONS = Map.of("hegemony", 1f, "tritachyon", 1f, "pirates", 1f);
    private static final AtlasCatalysts.Catalyst REDEPLOYMENT = new AtlasCatalysts.Catalyst("test_redeployment", "", AtlasCatalysts.Family.ORDERS,
            1f, 1, AtlasCatalysts.Effect.REDEPLOY, "graphics/test.png");

    private static AtlasGenerator generator(AtlasState state, Map<String, Float> factions) {
        AtlasRules rules = new AtlasRules(REGIONS, Map.of("compact", 1f), factions, new AtlasMarkerRule(0.3f, 0f, 0.3f),
                AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, List.of(), List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    private static AtlasCell system(AtlasGenerator generator, int skip) {
        int found = 0;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(6)) {
            if (!cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell) && found++ == skip) {
                return cell;
            }
        }
        throw new AssertionError("no system");
    }

    private static CraftingMaterials holding(int orders) {
        return CraftingMaterials.storedForTests(new HashMap<>(Map.of(REDEPLOYMENT.itemId(), orders)));
    }

    private static RedeploymentCatalystUse use(AtlasGenerator generator, AtlasState state, AtlasCell openRunCell, Set<AtlasCell> revealed) {
        return (RedeploymentCatalystUse) AtlasCatalystUse.of(REDEPLOYMENT, generator, state, openRunCell, revealed);
    }

    @Test
    void anOrderHandsTheSystemToAnotherFactionOfItsRegionPickedTheSameWayEveryTime() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, THREE_FACTIONS);
        AtlasCell cell = system(generator, 0);
        String owner = AtlasOpening.territoryOwnerOf(generator, state, cell);
        RedeploymentCatalystUse use = use(generator, state, null, Set.of(cell));
        String expected = use.newOwner(cell);
        AtlasState reloaded = new AtlasState(SEED);
        assertEquals(expected, use(generator(reloaded, THREE_FACTIONS), reloaded, null, Set.of(cell)).newOwner(cell));
        CraftingMaterials materials = holding(2);

        AtlasCatalystUse.Outcome outcome = Consumables.use(use, cell, materials);

        assertNotNull(outcome);
        assertNotEquals(owner, expected);
        assertTrue(THREE_FACTIONS.containsKey(expected), expected);
        assertEquals(expected, AtlasOpening.territoryOwnerOf(generator, state, cell));
        assertEquals(expected, AtlasOpening.ownerOf(generator, state, cell));
        assertEquals(Map.of("faction", RedeploymentCatalystUse.factionName(expected)), outcome.args());
        assertEquals(1, materials.count(REDEPLOYMENT.itemId()));
        assertNotNull(Consumables.use(use, cell, materials));
        assertNotEquals(expected, AtlasOpening.territoryOwnerOf(generator, state, cell), "a second order moves the system on");
    }

    @Test
    void anOrderRefusesFoggedSpentHeldAndEmptyCellsWithoutSpending() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, THREE_FACTIONS);
        AtlasCell open = system(generator, 0);
        AtlasCell completed = system(generator, 1);
        AtlasCell fogged = system(generator, 2);
        state.add(completed, AtlasState.COMPLETED);
        RedeploymentCatalystUse use = use(generator, state, open, Set.of(open, completed, AtlasCell.ORIGIN));
        CraftingMaterials materials = holding(1);

        assertEquals(AtlasCatalystUse.FOGGED_KEY, use.check(fogged).refusalKey());
        assertEquals("catalyst.refused.completed", use.check(completed).refusalKey());
        assertEquals(RedeploymentCatalystUse.OPEN_RUN_KEY, use.check(open).refusalKey());
        assertEquals("catalyst.refused.no_system", use.check(AtlasCell.ORIGIN).refusalKey());
        assertNull(Consumables.use(use, open, materials));
        assertEquals(1, materials.count(REDEPLOYMENT.itemId()));
        assertNull(state.capturedBy(open));
    }

    @Test
    void aRegionWithNoOtherFactionRefusesTheOrder() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, Map.of("pirates", 1f));
        AtlasCell cell = system(generator, 0);

        assertEquals(RedeploymentCatalystUse.NO_FACTION_KEY, use(generator, state, null, Set.of(cell)).check(cell).refusalKey());
    }
}
