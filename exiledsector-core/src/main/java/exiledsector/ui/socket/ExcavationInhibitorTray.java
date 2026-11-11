package exiledsector.ui.socket;

import exiledsector.items.CraftingInhibitor;
import exiledsector.items.CraftingMaterials;

import java.util.Arrays;
import java.util.List;

final class ExcavationInhibitorTray implements BenchTray {

    static final ExcavationInhibitorTray INSTANCE = new ExcavationInhibitorTray();

    private ExcavationInhibitorTray() {
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

    static List<BenchTrayEntry> held(CraftingMaterials materials) {
        return Arrays.stream(CraftingInhibitor.values()).filter(inhibitor -> materials.count(inhibitor.itemId()) > 0)
                .map(inhibitor -> (BenchTrayEntry) new InhibitorTrayEntry(inhibitor)).toList();
    }
}
