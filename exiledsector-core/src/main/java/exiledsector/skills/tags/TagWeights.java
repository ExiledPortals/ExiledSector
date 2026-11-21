package exiledsector.skills.tags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class TagWeights {

    public static final TagWeights NONE = new TagWeights(List.of());

    private static final float UNBIASED = 1f;
    private static final float BLOCKED = 0f;

    private final List<Map<String, Float>> multipliersByTagPerSource;

    private TagWeights(List<Map<String, Float>> multipliersByTagPerSource) {
        this.multipliersByTagPerSource = List.copyOf(multipliersByTagPerSource);
    }

    public static TagWeights of(List<Map<String, Float>> multipliersByTagPerSource) {
        TagWeights combined = NONE;
        for (Map<String, Float> multipliersByTag : multipliersByTagPerSource) {
            combined = combined.with(multipliersByTag);
        }
        return combined;
    }

    public TagWeights with(Map<String, Float> multipliersByTag) {
        multipliersByTag.forEach((tag, multiplier) -> {
            if (multiplier == null || multiplier.isNaN() || multiplier.isInfinite() || multiplier < BLOCKED) {
                throw new IllegalArgumentException("tag " + tag + " has the multiplier " + multiplier + "; expected a number of 0 or more");
            }
        });
        List<Map<String, Float>> sources = new ArrayList<>(multipliersByTagPerSource);
        sources.add(Map.copyOf(multipliersByTag));
        return new TagWeights(sources);
    }

    public boolean hasSources() {
        return !multipliersByTagPerSource.isEmpty();
    }

    public float weight(float poolWeight, Collection<String> expandedTags) {
        return hasSources() ? poolWeight * multiplier(expandedTags) : poolWeight;
    }

    public float multiplier(Collection<String> expandedTags) {
        float combinedMultiplier = UNBIASED;
        for (Map<String, Float> multipliersByTag : multipliersByTagPerSource) {
            float sourceMultiplier = sourceMultiplier(multipliersByTag, expandedTags);
            if (sourceMultiplier <= BLOCKED) {
                return BLOCKED;
            }
            combinedMultiplier *= sourceMultiplier;
        }
        return combinedMultiplier;
    }

    private static float sourceMultiplier(Map<String, Float> multipliersByTag, Collection<String> expandedTags) {
        Float strongest = null;
        for (String tag : expandedTags) {
            Float multiplier = multipliersByTag.get(tag);
            if (multiplier == null) {
                continue;
            }
            if (multiplier <= BLOCKED) {
                return BLOCKED;
            }
            if (strongest == null || isStronger(multiplier, strongest)) {
                strongest = multiplier;
            }
        }
        return strongest == null ? UNBIASED : strongest;
    }

    private static boolean isStronger(float multiplier, float strongest) {
        float strength = strength(multiplier);
        float strongestStrength = strength(strongest);
        return strength > strongestStrength || strength >= strongestStrength && multiplier > strongest;
    }

    private static float strength(float multiplier) {
        return multiplier >= UNBIASED ? multiplier : UNBIASED / multiplier;
    }
}
