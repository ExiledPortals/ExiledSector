package exiledsector.ui.socket;

import exiledsector.i18n.StyledText;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemDefinition;
import exiledsector.hullitems.HullItemName;
import exiledsector.hullitems.HullItemRarity;

import java.util.Locale;
import java.util.function.Function;

public record HullItemStorageRow(HullItem hullItem, int storeOrder, String name, HullItemRarity rarity, String searchText,
                               String installedShipName) {

    static HullItemStorageRow of(HullItem hullItem, int storeOrder, Function<HullItem, String> installedShipLookup) {
        HullItemDefinition definition = hullItem.definition();
        HullItemName displayName = hullItem.displayName();
        StringBuilder searchBuilder = new StringBuilder(displayName.title());
        if (displayName.baseName() != null) {
            searchBuilder.append('\n').append(displayName.baseName());
        }
        if (definition != null) {
            searchBuilder.append('\n').append(hullItem.kind().displayName());
        }
        for (StyledText line : hullItem.tooltipLines()) {
            searchBuilder.append('\n').append(line.plain());
        }
        return new HullItemStorageRow(hullItem, storeOrder, displayName.title(), displayName.rarity(),
                searchBuilder.toString().toLowerCase(Locale.ROOT), installedShipLookup.apply(hullItem));
    }

    boolean installed() {
        return installedShipName != null;
    }
}
