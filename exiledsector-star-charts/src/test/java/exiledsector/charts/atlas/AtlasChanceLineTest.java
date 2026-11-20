package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.DenseNebula;
import exiledsector.charts.anomaly.MagneticStorm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AtlasChanceLineTest {

    private static final float TOLERANCE = 0.0001f;

    @AfterEach
    void tearDown() {
        AtlasData.resetForTests();
        ChartTables.resetAnomalies();
    }

    private static AtlasRules shippedRules() throws Exception {
        ChartTables.loadShipped();
        ChartTables.loadFeatures();
        ChartTables.loadAnomalies();
        assertEquals(List.of(), AtlasData.registerAnomalyChances(ChartTables.csvRows(AtlasData.ANOMALY_CHANCES_PATH)));
        return AtlasData.rules();
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
        AtlasEffects eight = AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT, 8f)));
        AtlasAnomalyCandidate sharing = new AtlasAnomalyCandidate(DenseNebula.ANOMALY_ID, 1f, "", false, Set.of(), null, null);
        AtlasAnomalyCandidate owning = new AtlasAnomalyCandidate(DenseNebula.ANOMALY_ID, 1f, "", false, Set.of(), null, own);
        AtlasAnomalyCandidate other = new AtlasAnomalyCandidate(MagneticStorm.ANOMALY_ID, 1f, "", false, Set.of(), null, null);

        AtlasAnomalyChance increased = sharing.chanceRule(shared, eight);

        assertEquals(0.054f, increased.chance(), TOLERANCE);
        assertEquals(0.00216f, increased.chancePerStep(), TOLERANCE);
        assertEquals(0.162f, increased.maxChance(), TOLERANCE);
        assertEquals(0.108f, owning.chanceRule(shared, eight).chance(), TOLERANCE);
        assertEquals(shared, sharing.chanceRule(shared, AtlasEffects.NONE));
        assertEquals(own, owning.chanceRule(shared, AtlasEffects.NONE));
        assertEquals(shared, other.chanceRule(shared, eight));
    }

    @Test
    void theAtlasRaisesOnlyTheDenseNebulasChance() throws Exception {
        AtlasRules rules = shippedRules();
        AtlasEffects eight = AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT, 8f)));

        AtlasAnomalyChance plainNebula = candidate(rules, DenseNebula.ANOMALY_ID).chanceRule(rules.anomalyRule());
        AtlasAnomalyChance raisedNebula = candidate(rules, DenseNebula.ANOMALY_ID).chanceRule(rules.anomalyRule(), eight);

        assertEquals(plainNebula.chanceAt(0f) * 1.08f, raisedNebula.chanceAt(0f), TOLERANCE);
        assertEquals(plainNebula.chanceAt(1000f) * 1.08f, raisedNebula.chanceAt(1000f), TOLERANCE);
        for (AtlasAnomalyCandidate candidate : rules.anomalies()) {
            if (!candidate.id().equals(DenseNebula.ANOMALY_ID)) {
                assertEquals(candidate.chanceRule(rules.anomalyRule()), candidate.chanceRule(rules.anomalyRule(), eight), candidate.id());
            }
        }
    }
}
