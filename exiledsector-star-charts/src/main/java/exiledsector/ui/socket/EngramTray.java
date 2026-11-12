package exiledsector.ui.socket;

import exiledsector.items.CraftingMaterials;
import exiledsector.items.Engram;
import exiledsector.items.Engrams;
import exiledsector.modifiers.ModifierSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class EngramTray implements BenchTray {

    public static final EngramTray INSTANCE = new EngramTray();
    static final String PREFIXES_KEY = "chart.ui.bench.tray.engrams.prefixes";
    static final String SUFFIXES_KEY = "chart.ui.bench.tray.engrams.suffixes";

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

    @Override
    public int iconColumns() {
        return 2;
    }

    @Override
    public List<Column> columns(BenchTrayContext context, Predicate<BenchTrayEntry> shown) {
        return sided(Engrams.all(), context.materials(), shown);
    }

    static List<Column> sided(List<Engram> engrams, CraftingMaterials materials, Predicate<BenchTrayEntry> shown) {
        List<BenchTrayEntry> prefixes = new ArrayList<>();
        List<BenchTrayEntry> suffixes = new ArrayList<>();
        for (Engram engram : engrams) {
            BenchTrayEntry entry = BenchTrayEntry.craftingItem(engram);
            if (materials.count(engram.itemId()) > 0 && shown.test(entry)) {
                (grantsPrefix(engram) ? prefixes : suffixes).add(entry);
            }
        }
        return List.of(new Column(PREFIXES_KEY, prefixes), new Column(SUFFIXES_KEY, suffixes));
    }

    private static boolean grantsPrefix(Engram engram) {
        return engram.modifier() != null && engram.modifier().slot() == ModifierSlot.PREFIX;
    }

    static List<BenchTrayEntry> held(List<Engram> engrams, CraftingMaterials materials) {
        return engrams.stream().filter(engram -> materials.count(engram.itemId()) > 0).map(BenchTrayEntry::craftingItem).toList();
    }
}
