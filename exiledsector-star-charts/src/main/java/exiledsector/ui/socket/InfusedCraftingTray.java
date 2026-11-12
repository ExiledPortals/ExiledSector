package exiledsector.ui.socket;

import exiledsector.items.BiomeCraft;
import exiledsector.items.InfusedCraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InfusedCraftingTray implements BenchTray {

    enum Action {
        REFORGE,
        BIOME_REFORGE,
        EXCHANGE,
        BIOME_GRAFT
    }

    interface CraftRow extends BenchTrayEntry {

        int feedstockAmount();

        Action action();
    }

    static final Comparator<CraftRow> ROW_ORDER = Comparator.comparingInt(CraftRow::feedstockAmount).thenComparing(CraftRow::action);

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
    public List<String> iconPaths() {
        Set<String> iconPaths = new LinkedHashSet<>();
        for (BenchTrayEntry entry : allEntries()) {
            iconPaths.addAll(entry.iconPaths());
        }
        return List.copyOf(iconPaths);
    }

    @Override
    public List<BenchTrayEntry> entries(BenchTrayContext context) {
        return allEntries();
    }

    private static List<BenchTrayEntry> allEntries() {
        return List.copyOf(ordered());
    }

    static List<CraftRow> ordered() {
        List<CraftRow> rows = new ArrayList<>(offered());
        for (BiomeCraft craft : BiomeCraft.values()) {
            rows.add(new BiomeCraftEntry(craft));
        }
        rows.sort(ROW_ORDER);
        return rows;
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
