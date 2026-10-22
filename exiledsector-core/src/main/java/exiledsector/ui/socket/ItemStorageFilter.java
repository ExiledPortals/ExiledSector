package exiledsector.ui.socket;

import exiledsector.items.ItemKind;
import exiledsector.items.ItemRarity;

import java.util.EnumSet;
import java.util.Set;

public final class ItemStorageFilter {

    public enum Status {ALL, FREE, INSTALLED}

    static final ItemStorageFilter SESSION = new ItemStorageFilter();

    Status status = Status.FREE;
    String query = "";
    final Set<ItemRarity> rarities = EnumSet.noneOf(ItemRarity.class);
    final Set<ItemKind> socketTypes = EnumSet.noneOf(ItemKind.class);

    void setStatus(Status value) {
        status = value;
    }

    void toggleRarity(ItemRarity rarity) {
        if (!rarities.remove(rarity)) {
            rarities.add(rarity);
        }
    }

    void toggleSocketType(ItemKind socketType) {
        if (!socketTypes.remove(socketType)) {
            socketTypes.add(socketType);
        }
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
    }
}
