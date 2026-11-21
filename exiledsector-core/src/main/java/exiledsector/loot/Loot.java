package exiledsector.loot;

import exiledsector.items.ItemData;
import exiledsector.items.schematic.ShipSchematic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Loot(List<ItemData> items, List<ShipSchematic> schematics, Map<String, Integer> materials) {

    public static final Loot NONE = new Loot(List.of(), List.of(), Map.of());

    public Loot {
        items = List.copyOf(items);
        schematics = List.copyOf(schematics);
        materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
    }

    public boolean isEmpty() {
        return items.isEmpty() && schematics.isEmpty() && materials.isEmpty();
    }

    public Loot withItems(List<ItemData> replacedItems) {
        return new Loot(replacedItems, schematics, materials);
    }

    public Loot plus(List<ItemData> moreItems, List<ShipSchematic> moreSchematics, Map<String, Integer> moreMaterials) {
        if (moreItems.isEmpty() && moreSchematics.isEmpty() && moreMaterials.isEmpty()) {
            return this;
        }
        List<ItemData> allItems = new ArrayList<>(items);
        allItems.addAll(moreItems);
        List<ShipSchematic> allSchematics = new ArrayList<>(schematics);
        allSchematics.addAll(moreSchematics);
        Map<String, Integer> allMaterials = new LinkedHashMap<>(materials);
        moreMaterials.forEach((itemId, quantity) -> allMaterials.merge(itemId, quantity, Integer::sum));
        return new Loot(allItems, allSchematics, allMaterials);
    }

    public Loot plus(Loot other) {
        return plus(other.items(), other.schematics(), other.materials());
    }
}
