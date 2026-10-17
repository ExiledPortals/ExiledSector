package exiledsector.ui.socket;

import exiledsector.hullitems.HullItemType;
import exiledsector.hullitems.HullItemRarity;

import java.util.EnumSet;
import java.util.Set;

public final class HullItemStorageFilter {

    public enum Status {ALL, FREE, INSTALLED}

    static final HullItemStorageFilter SESSION = new HullItemStorageFilter();

    Status status = Status.FREE;
    String query = "";
    final Set<HullItemRarity> rarities = EnumSet.noneOf(HullItemRarity.class);
    final Set<HullItemType> socketTypes = EnumSet.noneOf(HullItemType.class);

    void setStatus(Status value) {
        status = value;
    }

    void toggleRarity(HullItemRarity rarity) {
        if (!rarities.remove(rarity)) {
            rarities.add(rarity);
        }
    }

    void toggleSocketType(HullItemType socketType) {
        if (!socketTypes.remove(socketType)) {
            socketTypes.add(socketType);
        }
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
    }
}
