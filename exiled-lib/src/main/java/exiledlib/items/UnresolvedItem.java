package exiledlib.items;

import java.util.List;

public final class UnresolvedItem extends Item {

    private Integer chartTier;
    private Integer chartQuality;
    private List<String> rolledNodeIds;

    public UnresolvedItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    public UnresolvedItem(Item source) {
        super(source);
        source.keepWhenUnresolved(this);
    }

    @Override
    public void keepWhenUnresolved(UnresolvedItem unresolved) {
        unresolved.keepChart(chartTier, chartQuality);
        unresolved.keepRolledNodeIds(rolledNodeIds);
    }

    public void keepChart(Integer tier, Integer quality) {
        chartTier = tier;
        chartQuality = quality;
    }

    public void keepRolledNodeIds(List<String> nodeIds) {
        rolledNodeIds = nodeIds;
    }

    @Override
    public ItemType type() {
        return null;
    }

    public int chartTier(int fallback) {
        return chartTier == null ? fallback : chartTier;
    }

    public int chartQuality(int fallback) {
        return chartQuality == null ? fallback : chartQuality;
    }

    public List<String> rolledNodeIds() {
        return rolledNodeIds;
    }
}
