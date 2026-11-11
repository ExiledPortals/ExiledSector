package exiledsector.ui.socket;

import exiledsector.items.CraftingMaterials;
import exiledsector.items.Engram;
import exiledsector.items.Engrams;

import java.util.List;

public final class EngramTray implements BenchTray {

    public static final EngramTray INSTANCE = new EngramTray();

    private EngramTray() {
    }

    @Override
    public String titleKey() {
        return "chart.ui.bench.tray.engrams.title";
    }

    @Override
    public String emptyKey() {
        return "chart.ui.bench.tray.engrams.empty";
    }

    @Override
    public List<String> iconPaths() {
        return Engrams.all().stream().map(engram -> CraftingItemTrayEntry.iconPath(engram.itemId())).toList();
    }

    @Override
    public List<BenchTrayEntry> entries(BenchTrayContext context) {
        return held(Engrams.all(), context.materials());
    }

    static List<BenchTrayEntry> held(List<Engram> engrams, CraftingMaterials materials) {
        return engrams.stream().filter(engram -> materials.count(engram.itemId()) > 0).map(BenchTrayEntry::craftingItem).toList();
    }
}
