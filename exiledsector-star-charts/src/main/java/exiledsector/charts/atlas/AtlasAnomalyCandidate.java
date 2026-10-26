package exiledsector.charts.atlas;

import java.util.Set;

public record AtlasAnomalyCandidate(String id, float weight, String requiredMod, boolean replacesStar, Set<String> excludes, Weighting weighting) {

    @FunctionalInterface
    public interface Weighting {
        float weightFor(AtlasTraits traits, float weight);
    }

    public AtlasAnomalyCandidate {
        weight = Math.max(0f, weight);
        requiredMod = requiredMod == null ? "" : requiredMod;
        excludes = excludes == null ? Set.of() : Set.copyOf(excludes);
        weighting = weighting == null ? (traits, base) -> base : weighting;
    }

    public AtlasAnomalyCandidate(String id, float weight, String requiredMod, Weighting weighting) {
        this(id, weight, requiredMod, false, Set.of(), weighting);
    }

    public AtlasAnomalyCandidate(String id, float weight, String requiredMod) {
        this(id, weight, requiredMod, null);
    }

    public boolean conflictsWith(AtlasAnomalyCandidate other) {
        return !id.equals(other.id) && (replacesStar && other.replacesStar || excludes.contains(other.id) || other.excludes.contains(id));
    }
}
