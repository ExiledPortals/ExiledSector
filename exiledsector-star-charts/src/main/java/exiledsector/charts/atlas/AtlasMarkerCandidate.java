package exiledsector.charts.atlas;

public record AtlasMarkerCandidate(String id, float weight, String requiredMod, Weighting weighting) {

    @FunctionalInterface
    public interface Weighting {
        float weightFor(AtlasTraits traits, float weight);
    }

    public AtlasMarkerCandidate {
        weight = Math.max(0f, weight);
        requiredMod = requiredMod == null ? "" : requiredMod;
        weighting = weighting == null ? (traits, base) -> base : weighting;
    }

    public AtlasMarkerCandidate(String id, float weight, String requiredMod) {
        this(id, weight, requiredMod, null);
    }
}
