package exiledsector.ui.socket;

import exiledsector.socketables.SocketableRarity;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SocketStorageFilter {

    public enum Status {ALL, FREE, INSTALLED}

    static final List<String> GRADES = List.of("consumer", "industrial", "military");

    static final SocketStorageFilter SESSION = new SocketStorageFilter();

    Status status = Status.FREE;
    String query = "";
    final Set<SocketableRarity> rarities = EnumSet.noneOf(SocketableRarity.class);
    final Set<String> grades = new LinkedHashSet<>();

    void setStatus(Status value) {
        status = value;
    }

    void toggleRarity(SocketableRarity rarity) {
        if (!rarities.remove(rarity)) {
            rarities.add(rarity);
        }
    }

    void toggleGrade(String grade) {
        if (!grades.remove(grade)) {
            grades.add(grade);
        }
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
    }
}
