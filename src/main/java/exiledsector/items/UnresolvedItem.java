package exiledsector.items;

import java.util.List;

public final class UnresolvedItem extends Item {

    private Integer chartTier;
    private Integer chartQuality;
    private String rolledNodeId;

    UnresolvedItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    UnresolvedItem(Item source) {
        super(source);
        if (source instanceof StarChart chart) {
            chartTier = chart.tier();
            chartQuality = chart.quality();
        } else if (source instanceof Subroutine subroutine) {
            rolledNodeId = subroutine.rolledNodeId();
        } else if (source instanceof UnresolvedItem unresolved) {
            chartTier = unresolved.chartTier;
            chartQuality = unresolved.chartQuality;
            rolledNodeId = unresolved.rolledNodeId;
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

    String rolledNodeId() {
        return rolledNodeId;
    }
}
