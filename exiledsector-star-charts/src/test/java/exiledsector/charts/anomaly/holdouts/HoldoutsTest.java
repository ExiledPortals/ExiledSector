package exiledsector.charts.anomaly.holdouts;

import exiledsector.charts.ChartTables;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.atlas.TestTerritories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoldoutsTest {

    private static final long SEED = 0x7e221L;
    private static final int SEARCH_RADIUS = 40;

    private AtlasState state;
    private AtlasGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
        List<AtlasRegion> regions = List.of(
                new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of()),
                new AtlasRegion("badlands", 4f, 0, "", "", Map.of(), Map.of("pirates", 5f, "independent", 1f), Map.of()));
        AtlasRules rules = new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 5f, "hegemony", 3f, "luddic_church", 2f),
                AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER, List.of(), List.of());
        state = new AtlasState(SEED);
        generator = new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
    }

    private AtlasCell systemIn(String regionId) {
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                AtlasCell cell = new AtlasCell(x, y);
                if (!cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell) && generator.specialAt(cell) == null
                        && generator.infestationAt(cell) == null && regionId.equals(generator.node(cell).regionId())) {
                    return cell;
                }
            }
        }
        throw new AssertionError("no " + regionId + " system");
    }

    @Test
    void anOutsideFactionHoldingABadlandsSystemFacesPirateHoldouts() {
        AtlasCell cell = systemIn("badlands");
        TestTerritories.establish(state, cell, "luddic_church");

        assertTrue(AtlasAnomalies.effective(generator, state, cell).contains(Holdouts.ANOMALY_ID));
        assertEquals("pirates", Holdouts.holdoutFactionAt(generator, state, cell));
        assertFalse(Holdouts.INSTANCE.atlasLines(generator, state, cell).isEmpty());
        AtlasRunNode joined = Holdouts.INSTANCE.joinRunNode(new AtlasRunNode(cell.pack(), "luddic_church", List.of(), List.of(Holdouts.ANOMALY_ID)),
                generator, state, cell, null);
        assertEquals("pirates", joined.anomalyData(Holdouts.ANOMALY_ID));
    }

    @Test
    void theRegionsOwnFactionsAndUnclaimedSystemsFaceNoHoldouts() {
        AtlasCell badlands = systemIn("badlands");
        assertFalse(AtlasAnomalies.effective(generator, state, badlands).contains(Holdouts.ANOMALY_ID));

        TestTerritories.establish(state, badlands, "pirates");

        assertFalse(AtlasAnomalies.effective(generator, state, badlands).contains(Holdouts.ANOMALY_ID));
        assertNull(Holdouts.holdoutFactionAt(generator, state, badlands));
    }

    @Test
    void systemsOutsideRegionsWithTheirOwnTerritoryFactionsFaceNoHoldouts() {
        AtlasCell normal = systemIn(AtlasRegion.NORMAL);
        TestTerritories.establish(state, normal, "luddic_church");

        assertFalse(AtlasAnomalies.effective(generator, state, normal).contains(Holdouts.ANOMALY_ID));
    }
}
