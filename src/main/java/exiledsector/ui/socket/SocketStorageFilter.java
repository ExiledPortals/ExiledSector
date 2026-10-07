package exiledsector.ui.socket;

import exiledsector.socketables.SocketableRarity;

import java.util.EnumSet;
import java.util.Set;

public final class SocketStorageFilter {

    public enum Status {ALL, FREE, INSTALLED}

    static final SocketStorageFilter SESSION = new SocketStorageFilter();

    Status status = Status.FREE;
    String query = "";
    final Set<SocketableRarity> rarities = EnumSet.noneOf(SocketableRarity.class);

    void setStatus(Status value) {
        status = value;
    }

    void toggleRarity(SocketableRarity rarity) {
        if (!rarities.remove(rarity)) {
            rarities.add(rarity);
        }
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
    }
}
