package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.StarCharts;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledlib.items.Item;
import exiledlib.items.ItemRarity;
import exiledlib.items.ItemStore;
import exiledsector.items.StarChart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class StarChartBrowser {

    public static final int LOWEST_TIER = StarChart.LOWEST_TIER;
    public static final int HIGHEST_TIER = StarChart.HIGHEST_TIER;

    private final int[] tierCounts = new int[HIGHEST_TIER + 1];
    private final Set<ItemRarity> rarityFilter = EnumSet.noneOf(ItemRarity.class);
    private List<Item> storedCharts = List.of();
    private List<Item> shownCharts = List.of();
    private int selectedTier = LOWEST_TIER;
    private Item selectedChart;
    private boolean tierChosen;

    public static int tierOf(Item chart) {
        return StarChart.clampTier(chart.tier());
    }

    public static int prepare(SectorAPI sector, StarChartBrowser browser) {
        int moved = sector == null ? 0 : StarCharts.absorbCharts(sector);
        ItemStore store = ItemStore.starChartsIfCreated();
        browser.reload(store == null ? List.of() : store.owned());
        return moved;
    }

    public static String movedNotice(int moved) {
        return moved > 0 ? I18n.forGameText(() -> Translation.msg("chart.ui.storage.moved").count(moved).text()) : null;
    }

    public void reload(List<Item> charts) {
        storedCharts = charts == null ? List.of() : List.copyOf(charts);
        Arrays.fill(tierCounts, 0);
        for (Item chart : storedCharts) {
            tierCounts[tierOf(chart)]++;
        }
        if (selectedChart != null && storedCharts.stream().noneMatch(chart -> chart == selectedChart)) {
            selectedChart = null;
        }
        if (selectedChart == null && !tierChosen && count(selectedTier) == 0) {
            selectedTier = lowestStockedTier();
        }
        rebuildShown();
    }

    public List<Item> storedCharts() {
        return storedCharts;
    }

    public int storedCount() {
        return storedCharts.size();
    }

    public int count(int tier) {
        return tier < LOWEST_TIER || tier > HIGHEST_TIER ? 0 : tierCounts[tier];
    }

    public int lowestStockedTier() {
        for (int tier = LOWEST_TIER; tier <= HIGHEST_TIER; tier++) {
            if (tierCounts[tier] > 0) {
                return tier;
            }
        }
        return LOWEST_TIER;
    }

    public int selectedTier() {
        return selectedTier;
    }

    public boolean selectTier(int tier) {
        int clamped = StarChart.clampTier(tier);
        tierChosen = true;
        if (clamped == selectedTier) {
            return false;
        }
        selectedTier = clamped;
        rebuildShown();
        return true;
    }

    public Item selectedChart() {
        return selectedChart;
    }

    public boolean isSelected(Item chart) {
        return chart != null && chart == selectedChart;
    }

    public boolean select(Item chart) {
        if (chart != null && storedCharts.stream().noneMatch(stored -> stored == chart)) {
            return false;
        }
        selectedChart = chart;
        if (chart != null && tierOf(chart) != selectedTier) {
            selectedTier = tierOf(chart);
            tierChosen = true;
            rebuildShown();
        }
        return true;
    }

    public boolean toggle(Item chart) {
        return select(isSelected(chart) ? null : chart);
    }

    public boolean showsRarity(ItemRarity rarity) {
        return rarityFilter.contains(rarity);
    }

    public void toggleRarity(ItemRarity rarity) {
        if (!rarityFilter.remove(rarity)) {
            rarityFilter.add(rarity);
        }
        rebuildShown();
    }

    public List<Item> shownCharts() {
        return shownCharts;
    }

    public int inSelectedTierCount() {
        return count(selectedTier);
    }

    private void rebuildShown() {
        List<Item> shown = new ArrayList<>();
        for (Item chart : storedCharts) {
            if (tierOf(chart) == selectedTier && (rarityFilter.isEmpty() || rarityFilter.contains(chart.rarity()))) {
                shown.add(chart);
            }
        }
        shownCharts = List.copyOf(shown);
    }
}
