package exiledsector.charts.anomaly.breach;

import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.layout.ChartGeometry;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class BreachRules {

    static final float BASE_MAX_RADIUS = 2400f;
    static final float MAX_RADIUS_PER_TIER = 100f;
    static final float OPEN_SECONDS = 60f;
    static final float COLLAPSE_SECONDS = 60f;
    static final float TRIGGER_REACH = 150f;
    static final float MIN_GROWTH_MULT = 0.1f;
    static final float SUPPLY_USE_PERCENT = 100f;
    static final float FUEL_LIGHT_YEARS_PER_DAY = 0.5f;
    static final float BASE_CACHES = 2f;
    static final float CACHES_PER_TIER = 1f / 3f;
    static final int MAX_CACHES = 12;
    static final float MIN_CACHE_DEPTH = 0.15f;
    static final float CACHE_DEPTH_RANGE = 0.8f;
    static final float CACHE_DEPTH_BIAS = 1.5f;
    static final float CACHE_MARGIN = 150f;
    static final int CACHE_PLACEMENT_ATTEMPTS = 8;
    static final float WARPED_CHANCE = 0.3f;
    static final float DRIFT_SPEED = 40f;
    static final float LIGHTNING_INTERVAL_SECONDS = 6f;
    static final float LIGHTNING_DAMAGE = 300f;
    static final float LIGHTNING_EMP = 600f;
    static final float INCURSION_GUARD_BOSS_SHARE = 1.1f;
    static final float MIN_INCURSION_DEPTH = 0.3f;
    static final float MAX_INCURSION_DEPTH = 0.8f;

    record Growth(float maxRadius, float openSeconds, float collapseSeconds, float holdSeconds) {

        Growth(float maxRadius, float openSeconds, float collapseSeconds) {
            this(maxRadius, openSeconds, collapseSeconds, 0f);
        }

        float radiusAt(float seconds) {
            if (seconds <= 0f || isCollapsed(seconds)) {
                return 0f;
            }
            if (seconds < openSeconds) {
                return maxRadius * ease(seconds / openSeconds);
            }
            if (seconds < collapseStart()) {
                return maxRadius;
            }
            return maxRadius * (1f - ease((seconds - collapseStart()) / collapseSeconds));
        }

        boolean isCollapsing(float seconds) {
            return seconds >= collapseStart();
        }

        boolean isCollapsed(float seconds) {
            return seconds >= collapseStart() + collapseSeconds;
        }

        float heldSeconds(float seconds) {
            return Math.max(0f, Math.min(holdSeconds, seconds - openSeconds));
        }

        private float collapseStart() {
            return openSeconds + Math.max(0f, holdSeconds);
        }

        private static float ease(float progress) {
            float clamped = Math.max(0f, Math.min(1f, progress));
            return clamped * clamped * (3f - 2f * clamped);
        }
    }

    record CacheSite(float x, float y, float depth) {
    }

    private BreachRules() {
    }

    static Growth growth(int tier, float growthMult) {
        return growth(tier, growthMult, 0f);
    }

    static Growth growth(int tier, float growthMult, float holdSeconds) {
        float maxRadius = BASE_MAX_RADIUS + MAX_RADIUS_PER_TIER * Math.max(0, tier);
        return new Growth(maxRadius, OPEN_SECONDS / Math.max(MIN_GROWTH_MULT, growthMult), COLLAPSE_SECONDS, Math.max(0f, holdSeconds));
    }

    static float drainMult(float heldDrainPercent, float heldSeconds) {
        return Math.max(0f, 1f + heldDrainPercent * heldSeconds / ChartGenContext.PERCENT);
    }

    static float warpedChance(float increasedPercent) {
        return Math.max(0f, Math.min(1f, WARPED_CHANCE * (1f + increasedPercent / ChartGenContext.PERCENT)));
    }

    static float battleWarpedChance(float increasedPercent, float alwaysWarpedShare) {
        float share = Math.max(0f, Math.min(1f, alwaysWarpedShare));
        return share + (1f - share) * warpedChance(increasedPercent);
    }

    static Vector2f drifted(Vector2f centre, Vector2f target, float seconds) {
        Vector2f towards = Vector2f.sub(target, centre, new Vector2f());
        float gap = towards.length();
        float step = DRIFT_SPEED * Math.max(0f, seconds);
        if (gap <= step) {
            return new Vector2f(target);
        }
        towards.scale(step / gap);
        return Vector2f.add(centre, towards, new Vector2f());
    }

    static float coreScale(float radius, boolean collapsing) {
        return collapsing ? Math.max(0f, Math.min(1f, radius / BreachCorePlugin.HALO_SIZE)) : 1f;
    }

    static boolean triggers(float distance, float fleetRadius) {
        return distance <= Math.max(0f, fleetRadius) + TRIGGER_REACH;
    }

    static boolean inside(float distance, float radius) {
        return radius > 0f && distance <= radius;
    }

    static int cacheCount(int tier, float cacheMult) {
        float expected = (BASE_CACHES + CACHES_PER_TIER * Math.max(0, tier)) * Math.max(0f, cacheMult);
        return Math.max(0, Math.min(MAX_CACHES, Math.round(expected)));
    }

    static List<CacheSite> cacheSites(Vector2f centre, float maxRadius, int count, Predicate<Vector2f> clear, Random random) {
        List<CacheSite> sites = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float deepest = Math.max(MIN_CACHE_DEPTH, Math.min(MIN_CACHE_DEPTH + CACHE_DEPTH_RANGE, (maxRadius - CACHE_MARGIN - 1f) / maxRadius));
            float depth = MIN_CACHE_DEPTH + (deepest - MIN_CACHE_DEPTH) * (float) Math.pow(random.nextFloat(), CACHE_DEPTH_BIAS);
            for (int attempt = 0; attempt < CACHE_PLACEMENT_ATTEMPTS; attempt++) {
                Vector2f at = ChartGeometry.pointAtRandomAngle(centre, depth * maxRadius, random);
                if (clear == null || clear.test(at)) {
                    sites.add(new CacheSite(at.x, at.y, depth));
                    break;
                }
            }
        }
        return sites;
    }

    static Vector2f incursionSite(Vector2f centre, float maxRadius, Predicate<Vector2f> clear, Random random) {
        for (int attempt = 0; attempt < CACHE_PLACEMENT_ATTEMPTS; attempt++) {
            float depth = MIN_INCURSION_DEPTH + (MAX_INCURSION_DEPTH - MIN_INCURSION_DEPTH) * random.nextFloat();
            Vector2f at = ChartGeometry.pointAtRandomAngle(centre, depth * maxRadius, random);
            if (clear == null || clear.test(at)) {
                return at;
            }
        }
        return null;
    }

    static boolean reached(float distance, float radius) {
        return distance <= radius - CACHE_MARGIN;
    }
}
