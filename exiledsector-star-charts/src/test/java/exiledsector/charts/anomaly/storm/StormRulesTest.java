package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StormRulesTest {

    private static ChartGenContext withEffect(ChartEffect effect, float value) {
        return ChartGenContext.none().withEffects(List.of(new ChartGenContext.AppliedEffect(effect, value)));
    }

    private static ShipAPI ship(HullSize hullSize) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getHullSize()).thenReturn(hullSize);
        when(ship.isFighter()).thenReturn(hullSize == HullSize.FIGHTER);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void strikesHitHarderEachTier() {
        assertEquals(StormRules.STRIKE_DAMAGE, StormRules.strikeDamage(1));
        assertEquals(StormRules.STRIKE_EMP * 1.4f, StormRules.strikeEmp(5), 0.01f);
        assertTrue(StormRules.strikeDamage(15) > StormRules.strikeDamage(14));
    }

    @Test
    void theChartLinesTuneTheStorm() {
        assertEquals(1.5f, StormRules.lootQuantityMult(ChartGenContext.none()), 0.001f);
        assertEquals(2f, StormRules.lootQuantityMult(withEffect(ChartEffect.CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT, 50f)), 0.001f);
        ChartGenContext faster = withEffect(ChartEffect.CHART_STORM_STRIKE_FREQUENCY_PERCENT, 100f);
        assertEquals(StormRules.STRIKE_SECONDS / 2f, StormRules.strikeSeconds(faster.anomalyMult(StormFront.STRIKE_KEY)), 0.001f);
        ChartGenContext wider = withEffect(ChartEffect.CHART_STORM_FRONT_SIZE_PERCENT, 50f);
        assertEquals(StormRules.BAND_WIDTH * 1.5f, StormRules.bandWidth(wider.anomalyMult(StormFront.SIZE_KEY)), 0.01f);
    }

    @Test
    void strikesSkipFightersPhasedShipsAndModules() {
        ShipAPI fighter = ship(HullSize.FIGHTER);
        ShipAPI phased = ship(HullSize.CRUISER);
        when(phased.isPhased()).thenReturn(true);
        ShipAPI station = ship(HullSize.CAPITAL_SHIP);
        ShipAPI module = ship(HullSize.CRUISER);
        when(module.getParentStation()).thenReturn(station);
        ShipAPI hulk = ship(HullSize.CAPITAL_SHIP);
        when(hulk.isHulk()).thenReturn(true);

        for (ShipAPI skipped : List.of(fighter, phased, module, hulk)) {
            assertFalse(StormStrikes.canBeStruck(skipped));
        }
        assertNull(StormStrikes.pickTarget(List.of(fighter, phased, module, hulk), new Random(1L)));
    }

    @Test
    void largerHullsAreStruckMoreOften() {
        ShipAPI frigate = ship(HullSize.FRIGATE);
        ShipAPI capital = ship(HullSize.CAPITAL_SHIP);
        Map<ShipAPI, Integer> hits = new HashMap<>();
        Random random = new Random(5L);
        for (int strike = 0; strike < 5000; strike++) {
            hits.merge(StormStrikes.pickTarget(List.of(frigate, capital), random), 1, Integer::sum);
        }

        assertEquals(4f, hits.get(capital) / (float) hits.get(frigate), 0.5f);
    }

    @Test
    void strikesStartAShortWayFromTheirTarget() {
        Random random = new Random(2L);
        for (int strike = 0; strike < 100; strike++) {
            float distance = StormStrikes.origin(new Vector2f(100f, 100f), random).length();
            assertTrue(distance >= StormStrikes.MIN_ORIGIN_DISTANCE - 150f && distance <= StormStrikes.MAX_ORIGIN_DISTANCE + 150f);
        }
    }
}
