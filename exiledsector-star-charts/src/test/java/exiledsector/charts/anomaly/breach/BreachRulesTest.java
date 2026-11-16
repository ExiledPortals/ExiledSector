package exiledsector.charts.anomaly.breach;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void effectsApplyEverywhereInsideTheFieldAndStopOutsideIt() {
        assertTrue(BreachRules.inside(0f, 1000f));
        assertTrue(BreachRules.inside(1000f, 1000f));
        assertFalse(BreachRules.inside(1001f, 1000f));
        assertFalse(BreachRules.inside(10f, 0f));
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
    void aCacheIsReachedOnlyOnceTheFieldPassesItsMargin() {
        assertTrue(BreachRules.reached(500f, 1000f));
        assertFalse(BreachRules.reached(950f, 1000f));
    }

    @Test
    void theCoreKeepsItsSizeUntilTheCollapsingFieldShrinksPastItsHaloThenContractsToNothing() {
        assertEquals(1f, BreachRules.coreScale(10f, false));
        assertEquals(1f, BreachRules.coreScale(BreachCorePlugin.HALO_SIZE * 3f, true));
        assertEquals(0.5f, BreachRules.coreScale(BreachCorePlugin.HALO_SIZE / 2f, true), 0.001f);
        assertEquals(0f, BreachRules.coreScale(0f, true));
    }

    @Test
    void aHoldKeepsTheBreachAtFullSizeAndDelaysTheCollapseByTheHold() {
        BreachRules.Growth held = new BreachRules.Growth(2000f, 60f, 60f, 30f);

        assertEquals(2000f, held.radiusAt(60f), 0.01f);
        assertEquals(2000f, held.radiusAt(89f), 0.01f);
        assertFalse(held.isCollapsing(89f));
        assertTrue(held.isCollapsing(90f));
        assertEquals(1000f, held.radiusAt(120f), 0.01f);
        assertFalse(held.isCollapsed(149f));
        assertTrue(held.isCollapsed(150f));
        assertEquals(0f, held.heldSeconds(30f));
        assertEquals(15f, held.heldSeconds(75f));
        assertEquals(30f, held.heldSeconds(140f));
        assertEquals(0f, new BreachRules.Growth(2000f, 60f, 60f).heldSeconds(100f));
        assertEquals(30f, BreachRules.growth(5, 1f, 30f).holdSeconds());
        assertEquals(0f, BreachRules.growth(5, 1f, -5f).holdSeconds());
    }

    @Test
    void theDrainRisesWithEverySecondHeldAndNeverGoesNegative() {
        assertEquals(1f, BreachRules.drainMult(3f, 0f));
        assertEquals(1.9f, BreachRules.drainMult(3f, 30f), 0.0001f);
        assertEquals(1f, BreachRules.drainMult(0f, 30f));
        assertEquals(0f, BreachRules.drainMult(-10f, 30f));
    }

    @Test
    void warpedChanceScalesTheBaseChanceAndAnAlwaysWarpedShareFillsTheRest() {
        assertEquals(BreachRules.WARPED_CHANCE, BreachRules.warpedChance(0f), 0.0001f);
        assertEquals(0.45f, BreachRules.warpedChance(50f), 0.0001f);
        assertEquals(1f, BreachRules.warpedChance(1000f));
        assertEquals(0f, BreachRules.warpedChance(-200f));
        assertEquals(0.6f, BreachRules.battleWarpedChance(100f, 0f), 0.0001f);
        assertEquals(0.8f, BreachRules.battleWarpedChance(100f, 0.5f), 0.0001f);
        assertEquals(1f, BreachRules.battleWarpedChance(0f, 1f), 0.0001f);
    }

    @Test
    void aDriftingBreachStepsTowardItsTargetWithoutOvershooting() {
        Vector2f stepped = BreachRules.drifted(new Vector2f(), new Vector2f(1000f, 0f), 2f);
        assertEquals(BreachRules.DRIFT_SPEED * 2f, stepped.x, 0.01f);
        assertEquals(0f, stepped.y, 0.01f);
        Vector2f arrived = BreachRules.drifted(new Vector2f(), new Vector2f(10f, 0f), 2f);
        assertEquals(10f, arrived.x, 0.01f);
    }

    @Test
    void theIncursionStationSitsInsideTheFullBreachAwayFromTheCoreAndCanBeBlocked() {
        Vector2f centre = new Vector2f(500f, 500f);
        for (long seed = 0L; seed < 50L; seed++) {
            Vector2f at = BreachRules.incursionSite(centre, 2000f, point -> true, new Random(seed));
            float distance = Vector2f.sub(at, centre, new Vector2f()).length();
            assertTrue(distance >= 2000f * BreachRules.MIN_INCURSION_DEPTH - 0.01f && distance <= 2000f * BreachRules.MAX_INCURSION_DEPTH + 0.01f);
            assertEquals(at, BreachRules.incursionSite(centre, 2000f, point -> true, new Random(seed)));
        }
        assertNull(BreachRules.incursionSite(centre, 2000f, point -> false, new Random(1L)));
    }
}
