package exiledsector.items;

import java.util.List;

public final class UnresolvedItem extends Item {

    private Integer chartTier;
    private Integer chartQuality;
    private List<String> rolledNodeIds;

    UnresolvedItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    UnresolvedItem(Item source) {
        super(source);
        source.keepWhenUnresolved(this);
    }

    @Override
    void keepWhenUnresolved(UnresolvedItem unresolved) {
        unresolved.keepChart(chartTier, chartQuality);
        unresolved.keepRolledNodeIds(rolledNodeIds);
    }

    void keepChart(Integer tier, Integer quality) {
        chartTier = tier;
        chartQuality = quality;
    }

    void keepRolledNodeIds(List<String> nodeIds) {
        rolledNodeIds = nodeIds;
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

    List<String> rolledNodeIds() {
        return rolledNodeIds;
    }
}
