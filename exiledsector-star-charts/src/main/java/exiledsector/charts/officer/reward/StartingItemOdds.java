package exiledsector.charts.officer.reward;

import exiledsector.data.CsvRows;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record StartingItemOdds(float uniqueSharePercent, float craftLow, float craftMid, float craftHigh) {

    public static final String UNIQUE_SHARE_COLUMN = "unique_share";
    public static final String CRAFT_LOW_COLUMN = "craft_low";
    public static final String CRAFT_MID_COLUMN = "craft_mid";
    public static final String CRAFT_HIGH_COLUMN = "craft_high";
    private static final float PERCENT = 100f;

    public StartingItemOdds {
        uniqueSharePercent = Math.max(0f, Math.min(PERCENT, uniqueSharePercent));
        craftLow = Math.max(0f, craftLow);
        craftMid = Math.max(0f, craftMid);
        craftHigh = Math.max(0f, craftHigh);
    }

    public static StartingItemOdds fromRow(JSONObject row, CsvRows csv, String rowLabel) {
        return new StartingItemOdds(csv.number(row, UNIQUE_SHARE_COLUMN, 0f, rowLabel), csv.number(row, CRAFT_LOW_COLUMN, 1f, rowLabel),
                csv.number(row, CRAFT_MID_COLUMN, 1f, rowLabel), csv.number(row, CRAFT_HIGH_COLUMN, 1f, rowLabel));
    }

    public float tierWeight(CraftingTier tier) {
        if (tier == null) {
            return 0f;
        }
        return switch (tier) {
            case LOW -> craftLow;
            case MID -> craftMid;
            case HIGH -> craftHigh;
        };
    }

    Map<RewardKind, Float> shares(Map<RewardKind, Float> poolShares) {
        float otherTotal = 0f;
        for (Map.Entry<RewardKind, Float> share : poolShares.entrySet()) {
            if (share.getKey() != RewardKind.UNIQUE) {
                otherTotal += Math.max(0f, share.getValue());
            }
        }
        Map<RewardKind, Float> shares = new EnumMap<>(RewardKind.class);
        float otherScale = otherTotal > 0f ? (PERCENT - uniqueSharePercent) / otherTotal : 0f;
        poolShares.forEach((kind, share) -> {
            if (kind != RewardKind.UNIQUE) {
                shares.put(kind, Math.max(0f, share) * otherScale);
            }
        });
        shares.put(RewardKind.UNIQUE, uniqueSharePercent);
        return shares;
    }

    Map<RewardKind, List<RewardCandidate>> reweighted(Map<RewardKind, List<RewardCandidate>> candidates) {
        Map<RewardKind, List<RewardCandidate>> reweighted = new EnumMap<>(RewardKind.class);
        reweighted.putAll(candidates);
        List<RewardCandidate> craftingItems = candidates.get(RewardKind.CRAFTING_ITEM);
        if (craftingItems != null) {
            reweighted.put(RewardKind.CRAFTING_ITEM, byTier(craftingItems));
        }
        return reweighted;
    }

    private List<RewardCandidate> byTier(List<RewardCandidate> craftingItems) {
        Map<CraftingTier, Float> tierTotals = new EnumMap<>(CraftingTier.class);
        for (RewardCandidate candidate : craftingItems) {
            CraftingTier tier = OfficerRewardTable.tierOf(candidate.item().id());
            if (tier != null) {
                tierTotals.merge(tier, Math.max(0f, candidate.weight()), Float::sum);
            }
        }
        List<RewardCandidate> reweighted = new ArrayList<>();
        for (RewardCandidate candidate : craftingItems) {
            CraftingTier tier = OfficerRewardTable.tierOf(candidate.item().id());
            float tierTotal = tier == null ? 0f : tierTotals.getOrDefault(tier, 0f);
            float weight = tierTotal > 0f ? tierWeight(tier) * Math.max(0f, candidate.weight()) / tierTotal : 0f;
            reweighted.add(candidate.withWeight(weight));
        }
        return reweighted;
    }
}
