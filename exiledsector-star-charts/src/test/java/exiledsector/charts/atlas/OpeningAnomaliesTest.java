package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningAnomaliesTest {

    private static final long SEED = 0x51A7E5L;
    private static final long CHART_SEED = 0xC0FFEEL;

    private static AtlasGenerator generator(AtlasState state, Map<String, Float> anomalyWeights) {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), anomalyWeights));
        AtlasRules rules = new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasAnomalyChance(0.3f, 0f, 0.3f),
                AtlasAnomalyChance.NEVER, List.of(), List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    private static AtlasCell system(AtlasGenerator generator) {
        for (AtlasCell cell : AtlasCell.ORIGIN.around(6)) {
            if (!cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell)) {
                return cell;
            }
        }
        throw new AssertionError("no system");
    }

    private static ChartAnomalies.Definition rolling(String id, float openingChance, Set<String> excludes) {
        return new ChartAnomalies.Definition(id, 1f, 1, 16, "", "", true, excludes, false, List.of(), Set.of(), 1f, openingChance);
    }

    private static ChartAnomalies.Definition rolling(String id, float openingChance) {
        return rolling(id, openingChance, Set.of());
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
    }

    @Test
    void anomaliesWithAnOpeningChanceJoinTheChartOnTopOfTheAtlasOnesAndOthersNever() {
        ChartTables.useAnomalies(List.of(rolling(SpatialBreach.ANOMALY_ID, 1f), rolling(AsteroidStorm.ANOMALY_ID, 1f),
                rolling(MagneticStorm.ANOMALY_ID, 0f), rolling(StellarFlares.ANOMALY_ID, 1f)));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, Map.of());
        AtlasCell cell = system(generator);

        List<String> rolled = OpeningAnomalies.rolled(generator, state, cell, CHART_SEED, List.of(StellarFlares.ANOMALY_ID));

        assertEquals(Set.of(SpatialBreach.ANOMALY_ID, AsteroidStorm.ANOMALY_ID), Set.copyOf(rolled));
        assertEquals(2, rolled.size());
        assertEquals(List.of(), OpeningAnomalies.rolled(generator, state, AtlasCell.ORIGIN, CHART_SEED, List.of()));
    }

    @Test
    void theRollKeepsTheSystemCapConflictsBiomeWeightsAndWhereAnAnomalyFits() {
        ChartTables.useAnomalies(List.of(rolling(SpatialBreach.ANOMALY_ID, 1f, Set.of(AsteroidStorm.ANOMALY_ID)), rolling(AsteroidStorm.ANOMALY_ID, 1f),
                rolling(FactionSkirmish.ANOMALY_ID, 1f)));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, Map.of());
        AtlasCell cell = system(generator);

        assertEquals(1, OpeningAnomalies.rolled(generator, state, cell, CHART_SEED, List.of()).size());
        List<String> full = new ArrayList<>();
        for (int filler = 0; filler < AtlasAnomalies.MAX_PER_SYSTEM; filler++) {
            full.add("filler_" + filler);
        }
        assertEquals(List.of(), OpeningAnomalies.rolled(generator, state, cell, CHART_SEED, full));
        AtlasGenerator barred = generator(state, Map.of(AtlasRegion.ANY_ANOMALY, 0f));
        assertEquals(List.of(), OpeningAnomalies.rolled(barred, state, system(barred), CHART_SEED, List.of()));
    }

    @Test
    void eachCommonAnomalyRollsAboutEightPercentOfOpeningsAndTheSameChartAlwaysRollsTheSame() throws Exception {
        ChartTables.loadAnomalies();
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, Map.of());
        AtlasCell cell = system(generator);
        int openings = 4000;
        int breaches = 0;

        for (long chartSeed = 0; chartSeed < openings; chartSeed++) {
            if (OpeningAnomalies.rolled(generator, state, cell, chartSeed, List.of()).contains(SpatialBreach.ANOMALY_ID)) {
                breaches++;
            }
        }

        float share = breaches / (float) openings;
        assertTrue(share > 0.06f && share < 0.10f, "breach share " + share);
        assertEquals(OpeningAnomalies.rolled(generator, state, cell, CHART_SEED, List.of()),
                OpeningAnomalies.rolled(generator, state, cell, CHART_SEED, List.of()));
    }
}
