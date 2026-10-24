package exiledsector.charts.atlas;

import java.util.List;
import java.util.Map;

public record AtlasRules(List<AtlasRegion> regions, Map<String, Float> layoutWeights, Map<String, Float> factionWeights,
                         AtlasMarkerRule voidRule, AtlasMarkerRule powerfulBossRule, AtlasMarkerRule expansionRule,
                         AtlasMarkerRule phenomenonRule, List<AtlasSpecial> specials, List<AtlasMarkerCandidate> expansions,
                         List<AtlasMarkerCandidate> phenomena) {

    public static final AtlasRules EMPTY = new AtlasRules(List.of(), Map.of(), Map.of(), AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER,
            AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, List.of(), List.of(), List.of());

    public AtlasRules {
        regions = List.copyOf(regions);
        layoutWeights = Map.copyOf(layoutWeights);
        factionWeights = Map.copyOf(factionWeights);
        specials = List.copyOf(specials);
        expansions = List.copyOf(expansions);
        phenomena = List.copyOf(phenomena);
    }

    public AtlasRegion normalRegion() {
        AtlasRegion normal = region(AtlasRegion.NORMAL);
        return normal == null ? AtlasRegion.DEFAULT_NORMAL : normal;
    }

    public AtlasRegion region(String id) {
        for (AtlasRegion region : regions) {
            if (region.id().equals(id)) {
                return region;
            }
        }
        return null;
    }

    public AtlasRules withExpansions(List<AtlasMarkerCandidate> replacement) {
        return new AtlasRules(regions, layoutWeights, factionWeights, voidRule, powerfulBossRule, expansionRule, phenomenonRule, specials,
                replacement, phenomena);
    }

    public AtlasRules withPhenomena(List<AtlasMarkerCandidate> replacement) {
        return new AtlasRules(regions, layoutWeights, factionWeights, voidRule, powerfulBossRule, expansionRule, phenomenonRule, specials,
                expansions, replacement);
    }
}
