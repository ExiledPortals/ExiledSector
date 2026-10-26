package exiledsector.charts.anomaly.breach;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreachRulesTest {

    @Test
    void theBreachOpensSlowlyFromNothingToItsMaximumThenCollapsesBackToNothing() {
        BreachRules.Growth growth = BreachRules.growth(5, 1f);
        float max = BreachRules.BASE_MAX_RADIUS + 5 * BreachRules.MAX_RADIUS_PER_TIER;
        float open = BreachRules.OPEN_SECONDS;
        float collapse = BreachRules.COLLAPSE_SECONDS;

        assertEquals(max, growth.maxRadius());
        assertEquals(open, growth.openSeconds());
        assertEquals(collapse, growth.collapseSeconds());
        assertEquals(0f, growth.radiusAt(0f));
        assertTrue(growth.radiusAt(open / 10f) < max * 0.05f);
        assertEquals(max * 0.5f, growth.radiusAt(open / 2f), 0.01f);
        assertEquals(max, growth.radiusAt(open), 0.01f);
        assertFalse(growth.isCollapsing(open - 1f));
        assertTrue(growth.isCollapsing(open));
        assertEquals(max * 0.5f, growth.radiusAt(open + collapse / 2f), 0.01f);
        assertFalse(growth.isCollapsed(open + collapse - 1f));
        assertTrue(growth.isCollapsed(open + collapse));
        assertEquals(0f, growth.radiusAt(open + collapse));
    }

    @Test
    void fasterGrowthOpensSoonerButCollapsesAtTheSamePaceAndNoGrowthIsFloored() {
        BreachRules.Growth normal = BreachRules.growth(1, 1f);
        BreachRules.Growth doubled = BreachRules.growth(1, 2f);
        BreachRules.Growth stalled = BreachRules.growth(1, 0f);

        assertEquals(normal.openSeconds() / 2f, doubled.openSeconds(), 0.001f);
        assertEquals(normal.collapseSeconds(), doubled.collapseSeconds());
        assertEquals(normal.openSeconds() / BreachRules.MIN_GROWTH_MULT, stalled.openSeconds(), 0.01f);
    }

    @Test
    void onlyAFleetPassingOverTheCoreOpensTheBreach() {
        assertTrue(BreachRules.triggers(100f, 50f));
        assertTrue(BreachRules.triggers(50f + BreachRules.TRIGGER_REACH, 50f));
        assertFalse(BreachRules.triggers(51f + BreachRules.TRIGGER_REACH, 50f));
    }

    @Test
    void effectsAreStrongestAtTheCoreAndStopOutsideTheField() {
        assertEquals(1f, BreachRules.intensity(0f, 1000f));
        assertEquals(BreachRules.EDGE_INTENSITY, BreachRules.intensity(1000f, 1000f), 0.001f);
        assertEquals(0f, BreachRules.intensity(1001f, 1000f));
        assertEquals(0f, BreachRules.intensity(10f, 0f));
        assertTrue(BreachRules.intensity(200f, 1000f) > BreachRules.intensity(800f, 1000f));
    }

    @Test
    void supplyUseRisesTowardTheCoreInWholeSteps() {
        assertEquals(BreachRules.SUPPLY_USE_PERCENT, BreachRules.supplyUsePercent(1f));
        assertEquals(25f, BreachRules.supplyUsePercent(BreachRules.EDGE_INTENSITY));
        assertEquals(0f, BreachRules.supplyUsePercent(0f));
        assertEquals(0f, BreachRules.supplyUsePercent(0.62f) % BreachRules.SUPPLY_USE_STEP);
    }

    @Test
    void higherTiersAndCacheBonusesFormMoreCachesUpToTheCap() {
        assertEquals(2, BreachRules.cacheCount(1, 1f));
        assertTrue(BreachRules.cacheCount(15, 1f) > BreachRules.cacheCount(1, 1f));
        assertTrue(BreachRules.cacheCount(5, 2f) > BreachRules.cacheCount(5, 1f));
        assertEquals(BreachRules.MAX_CACHES, BreachRules.cacheCount(15, 100f));
        assertEquals(0, BreachRules.cacheCount(15, 0f));
    }

    @Test
    void cacheSitesSitInsideTheFullBreachAndAreRepeatable() {
        Vector2f centre = new Vector2f(1000f, -500f);

        List<BreachRules.CacheSite> first = BreachRules.cacheSites(centre, 3000f, 8, point -> true, new Random(4L));
        List<BreachRules.CacheSite> second = BreachRules.cacheSites(centre, 3000f, 8, point -> true, new Random(4L));

        assertEquals(first, second);
        assertEquals(8, first.size());
        for (BreachRules.CacheSite site : first) {
            float distance = Vector2f.sub(new Vector2f(site.x(), site.y()), centre, new Vector2f()).length();
            assertEquals(site.depth() * 3000f, distance, 0.5f);
            assertTrue(site.depth() >= BreachRules.MIN_CACHE_DEPTH && site.depth() <= BreachRules.MIN_CACHE_DEPTH + BreachRules.CACHE_DEPTH_RANGE);
        }
    }

    @Test
    void everyCacheSitsWhereEvenTheSmallestFullBreachReachesIt() {
        for (BreachRules.CacheSite site : BreachRules.cacheSites(new Vector2f(), 1000f, 200, point -> true, new Random(2L))) {
            assertTrue(BreachRules.reached(site.depth() * 1000f, 1000f), "depth " + site.depth());
        }
    }

    @Test
    void cachesWithNoClearSpotAreLeftOut() {
        assertTrue(BreachRules.cacheSites(new Vector2f(), 3000f, 5, point -> false, new Random(1L)).isEmpty());
    }

    @Test
    void deeperCachesAreRarerAndFormOnlyOnceTheFieldReachesThem() {
        assertTrue(new BreachRules.CacheSite(0f, 0f, 0.2f).rarityPercent() > new BreachRules.CacheSite(0f, 0f, 0.9f).rarityPercent());
        assertTrue(BreachRules.reached(500f, 1000f));
        assertFalse(BreachRules.reached(950f, 1000f));
    }
}
