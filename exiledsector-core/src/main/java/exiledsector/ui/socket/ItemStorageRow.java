package exiledsector.ui.socket;

import exiledsector.i18n.StyledText;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemName;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemStatTotals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public record ItemStorageRow(Item item, int storeOrder, String name, ItemRarity rarity, List<String> searchLines,
                               String installedShipName, ItemStatTotals statTotals) {

    static ItemStorageRow of(Item item, int storeOrder, Function<Item, String> installedShipLookup) {
        ItemDefinition definition = item.definition();
        ItemName displayName = item.displayName();
        List<String> shownTexts = new ArrayList<>();
        shownTexts.add(displayName.title());
        if (displayName.baseName() != null) {
            shownTexts.add(displayName.baseName());
        }
        if (definition != null) {
            shownTexts.add(item.kindName());
        }
        for (StyledText line : item.tooltipLines()) {
            shownTexts.add(line.plain());
        }
        List<String> searchLines = shownTexts.stream().map(text -> text.toLowerCase(Locale.ROOT)).toList();
        return new ItemStorageRow(item, storeOrder, displayName.title(), displayName.rarity(), searchLines,
                installedShipLookup.apply(item), ItemStatTotals.of(item));
    }

    boolean installed() {
        return installedShipName != null;
    }
}
