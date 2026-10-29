package exiledsector.charts.atlas;

import java.util.Set;

public record AtlasAnomalyCandidate(String id, float weight, String requiredMod, boolean replacesStar, Set<String> excludes, Weighting weighting,
                                    AtlasMarkerRule ownChance, Set<String> biomes) {

    @FunctionalInterface
    public interface Weighting {
        float weightFor(AtlasTraits traits, float weight);
    }

    public AtlasAnomalyCandidate {
        weight = Math.max(0f, weight);
        requiredMod = requiredMod == null ? "" : requiredMod;
        excludes = excludes == null ? Set.of() : Set.copyOf(excludes);
        weighting = weighting == null ? (traits, base) -> base : weighting;
        biomes = biomes == null ? Set.of() : Set.copyOf(biomes);
    }

    public AtlasAnomalyCandidate(String id, float weight, String requiredMod, boolean replacesStar, Set<String> excludes, Weighting weighting,
                                 AtlasMarkerRule ownChance) {
        this(id, weight, requiredMod, replacesStar, excludes, weighting, ownChance, Set.of());
    }

    public AtlasAnomalyCandidate(String id, float weight, String requiredMod, boolean replacesStar, Set<String> excludes, Weighting weighting) {
        this(id, weight, requiredMod, replacesStar, excludes, weighting, null);
    }

    public AtlasAnomalyCandidate(String id, float weight, String requiredMod, Weighting weighting) {
        this(id, weight, requiredMod, false, Set.of(), weighting);
    }

    public AtlasAnomalyCandidate(String id, float weight, String requiredMod) {
        this(id, weight, requiredMod, null);
    }

    public static AtlasAnomalyCandidate perBiome(String id, String requiredMod, Set<String> biomes) {
        return new AtlasAnomalyCandidate(id, 0f, requiredMod, false, Set.of(), null, null, biomes);
    }

    public AtlasMarkerRule chanceRule(AtlasMarkerRule shared) {
        return ownChance == null ? shared : ownChance;
    }

    public boolean placedPerBiome() {
        return !biomes.isEmpty();
    }

    public boolean conflictsWith(AtlasAnomalyCandidate other) {
        return !id.equals(other.id) && (replacesStar && other.replacesStar || excludes.contains(other.id) || other.excludes.contains(id));
    }
}
