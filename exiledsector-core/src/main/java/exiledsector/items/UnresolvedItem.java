package exiledsector.items;

import java.util.List;

public final class UnresolvedItem extends Item {

    private Integer chartTier;
    private Integer chartQuantity;
    private List<String> rolledNodeIds;
    private String targetId;

    public UnresolvedItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    public UnresolvedItem(Item source) {
        super(source);
        source.keepWhenUnresolved(this);
    }

    @Override
    public void keepWhenUnresolved(UnresolvedItem unresolved) {
        unresolved.keepChart(chartTier, chartQuantity);
        unresolved.keepRolledNodeIds(rolledNodeIds);
        unresolved.keepTarget(targetId);
    }

    public void keepChart(Integer tier, Integer quantity) {
        chartTier = tier;
        chartQuantity = quantity;
    }

    public void keepRolledNodeIds(List<String> nodeIds) {
        rolledNodeIds = nodeIds;
    }

    public void keepTarget(String keptTargetId) {
        targetId = keptTargetId;
    }

    public String targetId() {
        return targetId;
    }

    @Override
    public ItemType type() {
        return null;
    }

    public int chartTier(int fallback) {
        return chartTier == null ? fallback : chartTier;
    }

    public int chartQuantity(int fallback) {
        return chartQuantity == null ? fallback : chartQuantity;
    }

    public List<String> rolledNodeIds() {
        return rolledNodeIds;
    }
}
