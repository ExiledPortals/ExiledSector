package exiledsector.ui.socket;

import exiledsector.items.InfusedCraft;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InfusedCraftingTray implements BenchTray {

    public static final InfusedCraftingTray INSTANCE = new InfusedCraftingTray();
    static final List<InfusedCraft> OFFERED_CRAFTS = List.of(InfusedCraft.REFORGE_WITH_TAG, InfusedCraft.ADD_AND_REMOVE);

    private InfusedCraftingTray() {
    }

    @Override
    public String titleKey() {
        return "chart.ui.bench.tray.infused.title";
    }

    @Override
    public String emptyKey() {
        return "chart.ui.bench.tray.infused.empty";
    }

    @Override
    public boolean listed() {
        return true;
    }

    @Override
    public List<String> iconPaths() {
        Set<String> feedstockIds = new LinkedHashSet<>();
        for (InfusedCraftEntry entry : offered()) {
            feedstockIds.add(entry.cost().feedstockId());
        }
        return feedstockIds.stream().map(CraftingItemTrayEntry::iconPath).toList();
    }

    @Override
    public List<BenchTrayEntry> entries(BenchTrayContext context) {
        return List.copyOf(offered());
    }

    static List<InfusedCraftEntry> offered() {
        Set<String> tags = new LinkedHashSet<>();
        OFFERED_CRAFTS.forEach(craft -> tags.addAll(craft.tags()));
        List<InfusedCraftEntry> entries = new ArrayList<>();
        for (String tag : tags) {
            for (InfusedCraft craft : OFFERED_CRAFTS) {
                if (craft.cost(tag) != null) {
                    entries.add(new InfusedCraftEntry(craft, tag));
                }
            }
        }
        return entries;
    }
}
