package exiledsector.ui.socket;

import exiledsector.i18n.StyledText;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableName;
import exiledsector.socketables.SocketableRarity;

import java.util.Locale;
import java.util.function.Function;

public record SocketStorageRow(Socketable socketable, int storeOrder, String name, SocketableRarity rarity, String searchText,
                               String installedShipName) {

    static SocketStorageRow of(Socketable socketable, int storeOrder, Function<Socketable, String> installedShipLookup) {
        SocketableDefinition definition = socketable.definition();
        SocketableName displayName = socketable.displayName();
        StringBuilder searchBuilder = new StringBuilder(displayName.title());
        if (displayName.baseName() != null) {
            searchBuilder.append('\n').append(displayName.baseName());
        }
        if (definition != null) {
            searchBuilder.append('\n').append(socketable.kind().displayName());
        }
        for (StyledText line : socketable.tooltipLines()) {
            searchBuilder.append('\n').append(line.plain());
        }
        return new SocketStorageRow(socketable, storeOrder, displayName.title(), displayName.rarity(),
                searchBuilder.toString().toLowerCase(Locale.ROOT), installedShipLookup.apply(socketable));
    }

    boolean installed() {
        return installedShipName != null;
    }
}
