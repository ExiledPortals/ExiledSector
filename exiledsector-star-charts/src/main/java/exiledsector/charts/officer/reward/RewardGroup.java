package exiledsector.charts.officer.reward;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public enum RewardGroup {
    LOW_CRAFTING("lowCrafting", RewardKind.CRAFTING_ITEM, CraftingTier.LOW),
    MID_CRAFTING("midCrafting", RewardKind.CRAFTING_ITEM, CraftingTier.MID),
    HIGH_CRAFTING("highCrafting", RewardKind.CRAFTING_ITEM, CraftingTier.HIGH),
    INHIBITOR("inhibitor", RewardKind.INHIBITOR, null),
    ENGRAM("engram", RewardKind.ENGRAM, null),
    FEEDSTOCK("feedstock", RewardKind.FEEDSTOCK, null),
    STAR_CHART("starChart", RewardKind.STAR_CHART, null),
    HULL_ITEM("hullItem", RewardKind.HULL_ITEM, null),
    BIOME_HULL_ITEM("biomeHullItem", RewardKind.BIOME_HULL_ITEM, null),
    UNIQUE("unique", RewardKind.UNIQUE, null);

    private static final String LABEL_KEY_PREFIX = "chart.officer.reward.group.";

    private final String id;
    private final RewardKind kind;
    private final CraftingTier tier;

    RewardGroup(String id, RewardKind kind, CraftingTier tier) {
        this.id = id;
        this.kind = kind;
        this.tier = tier;
    }

    public String id() {
        return id;
    }

    public RewardKind kind() {
        return kind;
    }

    public CraftingTier tier() {
        return tier;
    }

    public String labelKey() {
        return LABEL_KEY_PREFIX + id;
    }

    public boolean holds(RewardKind itemKind, CraftingTier itemTier) {
        return kind == itemKind && (tier == null || tier == itemTier);
    }

    public static RewardGroup of(CarriedItem item) {
        return of(item.kind(), OfficerRewardTable.tierOf(item.id()));
    }

    static RewardGroup of(RewardKind kind, CraftingTier tier) {
        RewardGroup fallback = null;
        for (RewardGroup group : values()) {
            if (group.holds(kind, tier)) {
                return group;
            }
            if (fallback == null && group.kind == kind) {
                fallback = group;
            }
        }
        return fallback;
    }

    public static Map<RewardGroup, Integer> entryCounts(Collection<CarriedItem> carried) {
        Map<RewardGroup, Integer> counts = new EnumMap<>(RewardGroup.class);
        for (CarriedItem item : carried) {
            counts.merge(of(item), item.entries(), Integer::sum);
        }
        return counts;
    }
}
