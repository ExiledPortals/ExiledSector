package exiledsector.charts.anomaly;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMarkerRule;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerfulBossTest {

    private static final long SEED = 0xB055L;
    private static final AtlasRules RULES = new AtlasRules(List.of(AtlasRegion.DEFAULT_NORMAL), Map.of("compact", 1f), Map.of("pirates", 1f),
            AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, List.of(), List.of());

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
    }

    @Test
    void theShippedRowMakesTheBossBiggerAndRicherAndAnyRunHoldingItGuaranteesTheTierUp() {
        ChartAnomalies.Definition definition = ChartAnomalies.definition(PowerfulBoss.ANOMALY_ID);
        assertNotNull(definition);
        assertTrue(definition.insertable());
        assertFalse(definition.icon().isEmpty());
        assertEquals(Set.of(ChartEffect.CHART_BOSS_SIZE_PERCENT, ChartEffect.CHART_BOSS_SPOILS_PERCENT),
                Set.copyOf(definition.effects().stream().map(ChartGenContext.AppliedEffect::effect).toList()));

        ChartGenContext powerful = ChartGenContext.none().withAnomalyEffects(List.of(SpatialBreach.ANOMALY_ID, PowerfulBoss.ANOMALY_ID));

        assertTrue(powerful.guaranteedBossTierUp());
        assertTrue(powerful.bossSizePercent() > 0f);
        assertTrue(powerful.bossSpoilsMult() > 1f);
        assertFalse(ChartGenContext.none().withAnomalyEffects(List.of(SpatialBreach.ANOMALY_ID)).guaranteedBossTierUp());
        assertTrue(ChartAnomalies.guaranteeBossTierUp(List.of(PowerfulBoss.ANOMALY_ID)));
        assertFalse(ChartAnomalies.guaranteeBossTierUp(List.of(SpatialBreach.ANOMALY_ID)));
    }

    @Test
    void flagshipOrdersAddAPowerfulBossThatTheRunNodeCarries() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state);
        AtlasCell cell = AtlasCell.ORIGIN.around(6).stream().filter(candidate -> !candidate.equals(AtlasCell.ORIGIN) && !generator.isVoid(candidate))
                .findFirst().orElseThrow();
        assertFalse(AtlasOpening.runNode(generator, state, cell).guaranteedBossTierUp());
        AtlasCatalysts.Catalyst flagshipOrders = new AtlasCatalysts.Catalyst("test_flagship_orders", PowerfulBoss.ANOMALY_ID, AtlasCatalysts.Family.ORDERS,
                1f, 1);
        AtlasCatalystUse use = AtlasCatalystUse.of(flagshipOrders, generator, state, null, Set.of(cell));

        assertTrue(use.check(cell).allowed());
        assertNotNull(use.apply(cell));
        AtlasRunNode node = AtlasOpening.runNode(generator, state, cell);

        assertEquals(List.of(PowerfulBoss.ANOMALY_ID), node.anomalyIds());
        assertTrue(node.guaranteedBossTierUp());
        assertEquals("catalyst.refused.already_present", use.check(cell).refusalKey());
    }
}
