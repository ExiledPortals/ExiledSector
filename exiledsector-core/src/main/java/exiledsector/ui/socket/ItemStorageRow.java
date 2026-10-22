package exiledsector.ui.socket;

import exiledlib.i18n.StyledText;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemName;
import exiledsector.items.ItemRarity;

import java.util.Locale;
import java.util.function.Function;

public record ItemStorageRow(Item item, int storeOrder, String name, ItemRarity rarity, String searchText,
                               String installedShipName) {

    static ItemStorageRow of(Item item, int storeOrder, Function<Item, String> installedShipLookup) {
        ItemDefinition definition = item.definition();
        ItemName displayName = item.displayName();
        StringBuilder searchBuilder = new StringBuilder(displayName.title());
        if (displayName.baseName() != null) {
            searchBuilder.append('\n').append(displayName.baseName());
        }
        if (definition != null) {
            searchBuilder.append('\n').append(item.kindName());
        }
        for (StyledText line : item.tooltipLines()) {
            searchBuilder.append('\n').append(line.plain());
        }
        return new ItemStorageRow(item, storeOrder, displayName.title(), displayName.rarity(),
                searchBuilder.toString().toLowerCase(Locale.ROOT), installedShipLookup.apply(item));
    }

    boolean installed() {
        return installedShipName != null;
    }
}
