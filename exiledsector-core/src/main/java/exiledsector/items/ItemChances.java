package exiledsector.items;

import exiledsector.loot.DropType;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;

import java.util.EnumMap;
import java.util.Map;

public record ItemChances(Map<DropType, Float> nodePercents, Map<DropType, Float> rarityPercents, float share) {

    public static final ItemChances BASE = new ItemChances(Map.of(), Map.of(), 1f);

    public ItemChances {
        nodePercents = nodePercents == null ? Map.of() : Map.copyOf(nodePercents);
        rarityPercents = rarityPercents == null ? Map.of() : Map.copyOf(rarityPercents);
        share = Math.max(0f, share);
    }

    public static ItemChances atShare(float share) {
        return share == 1f ? BASE : new ItemChances(Map.of(), Map.of(), share);
    }

    public static ItemChances increasedBy(DropType node, float increasedPercent) {
        return BASE.plusPercent(node, increasedPercent);
    }

    public float nodePercent(DropType node) {
        return percentAt(nodePercents, node);
    }

    public float rarityPercent(DropType node) {
        return percentAt(rarityPercents, node);
    }

    public float increasedPercent(DropType type) {
        float increasedPercent = 0f;
        for (DropType node : type.chain()) {
            increasedPercent += nodePercent(node) + FoundItemQuantity.nodePercent(node);
        }
        return increasedPercent;
    }

    public float increasedRarity(DropType type) {
        float increasedRarity = 0f;
        for (DropType node : type.chain()) {
            increasedRarity += rarityPercent(node);
        }
        return increasedRarity;
    }

    public float quantity(DropType type) {
        return new ItemQuantity(increasedPercent(type), share).mult();
    }

    public float rarity(DropType type) {
        return Math.max(0f, 1f + increasedRarity(type) / 100f);
    }

    public float extraOverParent(DropType type) {
        return type.parent() == null ? 0f : Math.max(0f, quantity(type) - quantity(type.parent()));
    }

    public ItemChances plusPercent(DropType node, float extraIncreasedPercent) {
        if (extraIncreasedPercent == 0f) {
            return this;
        }
        return new ItemChances(plus(nodePercents, node, extraIncreasedPercent), rarityPercents, share);
    }

    public ItemChances plusRarityPercent(DropType node, float extraIncreasedRarity) {
        if (extraIncreasedRarity == 0f) {
            return this;
        }
        return new ItemChances(nodePercents, plus(rarityPercents, node, extraIncreasedRarity), share);
    }

    public ItemChances scaled(float factor) {
        return factor == 1f ? this : new ItemChances(nodePercents, rarityPercents, share * factor);
    }

    private static float percentAt(Map<DropType, Float> percents, DropType node) {
        Float percent = percents.get(node);
        return percent == null ? 0f : percent;
    }

    private static Map<DropType, Float> plus(Map<DropType, Float> percents, DropType node, float extra) {
        Map<DropType, Float> summed = new EnumMap<>(DropType.class);
        summed.putAll(percents);
        summed.merge(node, extra, Float::sum);
        return summed;
    }
}
