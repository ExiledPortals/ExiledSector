package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class ChartSupply {

    public static final int COMBINE_COUNT = 3;
    public static final int VENDOR_TIER = ChartTiers.LOWEST_TIER;
    public static final int VENDOR_PRICE = 15000;

    private ChartSupply() {
    }

    public static List<Item> combinable(ItemStore store, int tier, Item kept) {
        if (store == null || tier >= ChartTiers.HIGHEST_TIER) {
            return List.of();
        }
        List<Item> candidates = new ArrayList<>();
        for (Item item : store.owned()) {
            if (item instanceof StarChart chart && chart.tier() == tier && item != kept && (chart.definition() == null || !chart.definition().unique())) {
                candidates.add(item);
            }
        }
        candidates.sort(Comparator.comparingInt((Item item) -> item.rarity().ordinal()).thenComparingInt(item -> item.explicits().size())
                .thenComparing(Item::id));
        return candidates.size() < COMBINE_COUNT ? List.of() : List.copyOf(candidates.subList(0, COMBINE_COUNT));
    }

    public static Item combine(ItemStore store, int tier, Item kept, Random random) {
        List<Item> spent = combinable(store, tier, kept);
        ItemDefinition definition = spent.isEmpty() ? null : ChartDrops.pickChartAtTier(random, tier + 1, candidate -> !candidate.unique());
        if (definition == null) {
            return null;
        }
        spent.forEach(store::remove);
        return store.add(ItemData.rolledAtTier(definition, random.nextLong(), tier + 1));
    }

    public static boolean canAfford(SectorAPI sector) {
        return sector != null && sector.getPlayerFleet() != null && sector.getPlayerFleet().getCargo().getCredits().get() >= VENDOR_PRICE;
    }

    public static Item buyFromGateway(SectorAPI sector, Random random) {
        if (!canAfford(sector)) {
            return null;
        }
        ItemDefinition definition = ChartDrops.pickChartAtTier(random, VENDOR_TIER, candidate -> !candidate.unique());
        if (definition == null) {
            return null;
        }
        sector.getPlayerFleet().getCargo().getCredits().subtract(VENDOR_PRICE);
        return ItemStore.starCharts().add(ItemData.rolledAtTier(definition, random.nextLong(), VENDOR_TIER));
    }
}
