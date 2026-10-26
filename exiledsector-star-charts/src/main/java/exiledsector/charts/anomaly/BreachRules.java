package exiledsector.charts.anomaly;

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
    static final float EDGE_INTENSITY = 0.25f;
    static final float SUPPLY_USE_PERCENT = 100f;
    static final float SUPPLY_USE_STEP = 5f;
    static final float FUEL_LIGHT_YEARS_PER_DAY = 0.5f;
    static final float BASE_CACHES = 2f;
    static final float CACHES_PER_TIER = 1f / 3f;
    static final int MAX_CACHES = 12;
    static final float MIN_CACHE_DEPTH = 0.15f;
    static final float CACHE_DEPTH_RANGE = 0.8f;
    static final float CACHE_DEPTH_BIAS = 1.5f;
    static final float CACHE_MARGIN = 150f;
    static final int CACHE_PLACEMENT_ATTEMPTS = 8;
    static final float CACHE_BASE_RARITY_PERCENT = 25f;
    static final float CACHE_DEEP_RARITY_PERCENT = 75f;
    static final float TOUCHED_RARITY_PERCENT = 30f;
    static final float CORE_MIN_ITEM_MULT = 0.5f;
    static final float CORE_GROWN_ITEM_MULT = 1.5f;
    static final float CORE_SALVAGE_MULT_PER_TIER = 0.2f;
    static final float CORE_BASE_RARITY_PERCENT = 50f;
    static final float CORE_GROWN_RARITY_PERCENT = 50f;

    record Growth(float maxRadius, float openSeconds, float collapseSeconds) {

        float radiusAt(float seconds) {
            if (seconds <= 0f || isCollapsed(seconds)) {
                return 0f;
            }
            if (seconds < openSeconds) {
                return maxRadius * ease(seconds / openSeconds);
            }
            return maxRadius * (1f - ease((seconds - openSeconds) / collapseSeconds));
        }

        boolean isCollapsing(float seconds) {
            return seconds >= openSeconds;
        }

        boolean isCollapsed(float seconds) {
            return seconds >= openSeconds + collapseSeconds;
        }

        float grownFraction(float radius) {
            return maxRadius <= 0f ? 1f : Math.max(0f, Math.min(1f, radius / maxRadius));
        }

        private static float ease(float progress) {
            float clamped = Math.max(0f, Math.min(1f, progress));
            return clamped * clamped * (3f - 2f * clamped);
        }
    }

    record CacheSite(float x, float y, float depth) {

        float rarityPercent() {
            return CACHE_BASE_RARITY_PERCENT + CACHE_DEEP_RARITY_PERCENT * (1f - depth);
        }
    }

    private BreachRules() {
    }

    static Growth growth(int tier, float growthMult) {
        float maxRadius = BASE_MAX_RADIUS + MAX_RADIUS_PER_TIER * Math.max(0, tier);
        return new Growth(maxRadius, OPEN_SECONDS / Math.max(MIN_GROWTH_MULT, growthMult), COLLAPSE_SECONDS);
    }

    static boolean triggers(float distance, float fleetRadius) {
        return distance <= Math.max(0f, fleetRadius) + TRIGGER_REACH;
    }

    static float intensity(float distance, float radius) {
        if (radius <= 0f || distance > radius) {
            return 0f;
        }
        float depth = 1f - Math.max(0f, distance) / radius;
        return EDGE_INTENSITY + (1f - EDGE_INTENSITY) * depth;
    }

    static float supplyUsePercent(float intensity) {
        return Math.round(SUPPLY_USE_PERCENT * Math.max(0f, Math.min(1f, intensity)) / SUPPLY_USE_STEP) * SUPPLY_USE_STEP;
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
                double angle = random.nextDouble() * Math.PI * 2d;
                Vector2f at = new Vector2f(centre.x + (float) Math.cos(angle) * depth * maxRadius, centre.y + (float) Math.sin(angle) * depth * maxRadius);
                if (clear == null || clear.test(at)) {
                    sites.add(new CacheSite(at.x, at.y, depth));
                    break;
                }
            }
        }
        return sites;
    }

    static boolean reached(float distance, float radius) {
        return distance <= radius - CACHE_MARGIN;
    }

    static float coreItemMult(float grownFraction, float coreItemMult) {
        return (CORE_MIN_ITEM_MULT + CORE_GROWN_ITEM_MULT * Math.max(0f, Math.min(1f, grownFraction))) * Math.max(0f, coreItemMult);
    }

    static float coreRarityPercent(float grownFraction) {
        return CORE_BASE_RARITY_PERCENT + CORE_GROWN_RARITY_PERCENT * Math.max(0f, Math.min(1f, grownFraction));
    }

    static float coreSalvageMult(int tier, float itemMult) {
        return itemMult * (1f + CORE_SALVAGE_MULT_PER_TIER * Math.max(0, tier - 1));
    }
}
