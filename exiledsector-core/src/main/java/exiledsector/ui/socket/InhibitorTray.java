package exiledsector.ui.socket;

import exiledsector.items.CraftingInhibitor;
import exiledsector.items.CraftingMaterials;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

final class InhibitorTray implements BenchTray {

    static final InhibitorTray INSTANCE = new InhibitorTray();
    static final int COLUMN_COUNT = 2;

    private InhibitorTray() {
    }

    @Override
    public String titleKey() {
        return "ui.craftingBench.tray.inhibitors.title";
    }

    @Override
    public String emptyKey() {
        return "ui.craftingBench.tray.inhibitors.empty";
    }

    @Override
    public List<String> iconPaths() {
        return Arrays.stream(CraftingInhibitor.values()).map(inhibitor -> CraftingItemTrayEntry.iconPath(inhibitor.itemId())).toList();
    }

    @Override
    public List<BenchTrayEntry> entries(BenchTrayContext context) {
        return held(context.materials());
    }

    @Override
    public int iconColumns() {
        return COLUMN_COUNT;
    }

    @Override
    public List<Column> columns(BenchTrayContext context, Predicate<BenchTrayEntry> shown) {
        return Column.dealt(entries(context).stream().filter(shown).toList(), COLUMN_COUNT);
    }

    static List<BenchTrayEntry> held(CraftingMaterials materials) {
        return Arrays.stream(CraftingInhibitor.values()).filter(inhibitor -> materials.count(inhibitor.itemId()) > 0)
                .map(inhibitor -> (BenchTrayEntry) new InhibitorTrayEntry(inhibitor)).toList();
    }
}
