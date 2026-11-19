package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.DenseNebula;
import exiledsector.charts.anomaly.MagneticStorm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasChanceLineTest {

    private static final float TOLERANCE = 0.0001f;

    @AfterEach
    void tearDown() {
        AtlasData.resetForTests();
        ChartTables.resetAnomalies();
    }

    private static AtlasRules shippedRules(AtlasEffects effects) throws Exception {
        ChartTables.loadShipped();
        ChartTables.loadFeatures();
        ChartTables.loadAnomalies();
        assertEquals(List.of(), AtlasData.registerAnomalyChances(ChartTables.csvRows(AtlasData.ANOMALY_CHANCES_PATH)));
        return AtlasData.rules(effects);
    }

    private static AtlasAnomalyCandidate candidate(AtlasRules rules, String anomalyId) {
        return rules.anomalies().stream().filter(candidate -> candidate.id().equals(anomalyId)).findFirst().orElseThrow();
    }

    @Test
    void theDenseNebulaChanceLineCountsOnlyForTheNebula() {
        AtlasEffects effects = AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT, 8f),
                new AtlasEffects.Line(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT, 8f), new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 1f)));

        assertEquals(16f, effects.atlasChancePercent(DenseNebula.ANOMALY_ID), TOLERANCE);
        assertEquals(0f, effects.atlasChancePercent(MagneticStorm.ANOMALY_ID), TOLERANCE);
        assertEquals(DenseNebula.ANOMALY_ID, AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT.atlasChanceAnomalyId());
        assertNull(AtlasModifier.ATLAS_VISIBILITY_LANES.atlasChanceAnomalyId());
    }

    @Test
    void anIncreasedChanceScalesTheBaseChanceItsGrowthAndItsCap() {
        AtlasAnomalyChance shared = new AtlasAnomalyChance(0.05f, 0.002f, 0.15f);
        AtlasAnomalyChance own = new AtlasAnomalyChance(0.1f, 0f, 0.1f);

        AtlasAnomalyChance increased = AtlasData.increasedChance(null, shared, 8f);

        assertEquals(0.054f, increased.chance(), TOLERANCE);
        assertEquals(0.00216f, increased.chancePerStep(), TOLERANCE);
        assertEquals(0.162f, increased.maxChance(), TOLERANCE);
        assertEquals(0.108f, AtlasData.increasedChance(own, shared, 8f).chance(), TOLERANCE);
        assertNull(AtlasData.increasedChance(null, shared, 0f));
        assertEquals(own, AtlasData.increasedChance(own, shared, 0f));
    }

    @Test
    void theAtlasRaisesOnlyTheDenseNebulasChance() throws Exception {
        AtlasRules plain = shippedRules(AtlasEffects.NONE);
        AtlasRules raised = AtlasData.rules(AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT, 8f))));

        AtlasAnomalyChance plainNebula = candidate(plain, DenseNebula.ANOMALY_ID).chanceRule(plain.anomalyRule());
        AtlasAnomalyChance raisedNebula = candidate(raised, DenseNebula.ANOMALY_ID).chanceRule(raised.anomalyRule());

        assertEquals(plainNebula.chanceAt(0f) * 1.08f, raisedNebula.chanceAt(0f), TOLERANCE);
        assertEquals(plainNebula.chanceAt(1000f) * 1.08f, raisedNebula.chanceAt(1000f), TOLERANCE);
        assertTrue(plain.anomalies().stream().allMatch(candidate -> candidate.ownChance() == null));
        for (AtlasAnomalyCandidate candidate : raised.anomalies()) {
            if (!candidate.id().equals(DenseNebula.ANOMALY_ID)) {
                assertNull(candidate.ownChance(), candidate.id());
            }
        }
    }
}
