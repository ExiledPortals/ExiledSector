package exiledsector.items;

import java.util.List;

public final class UnresolvedItem extends Item {

    private Integer chartTier;
    private Integer chartQuality;

    UnresolvedItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    UnresolvedItem(Item source) {
        super(source);
        if (source instanceof StarChart chart) {
            chartTier = chart.tier();
            chartQuality = chart.quality();
        } else if (source instanceof UnresolvedItem unresolved) {
            chartTier = unresolved.chartTier;
            chartQuality = unresolved.chartQuality;
        }
    }

    @Override
    public ItemType type() {
        return null;
    }

    int chartTier(int fallback) {
        return chartTier == null ? fallback : chartTier;
    }

    int chartQuality(int fallback) {
        return chartQuality == null ? fallback : chartQuality;
    }
}
