package exiledsector.ui.socket;

import exiledsector.items.ItemKind;
import exiledsector.items.ItemRarity;

import java.util.EnumSet;
import java.util.Set;

public final class ItemStorageFilter {

    public enum Status {FREE, INSTALLED}

    public enum Sort {NEWEST, OLDEST, RAREST}

    static final ItemStorageFilter SESSION = new ItemStorageFilter();

    final Set<Status> statuses = EnumSet.of(Status.FREE);
    String query = "";
    Sort sort = Sort.NEWEST;
    final Set<ItemRarity> rarities = EnumSet.noneOf(ItemRarity.class);
    final Set<ItemKind> sectionKinds = EnumSet.noneOf(ItemKind.class);

    void toggleStatus(Status status) {
        toggle(statuses, status);
    }

    void setSort(Sort chosen) {
        sort = chosen;
    }

    void toggleRarity(ItemRarity rarity) {
        toggle(rarities, rarity);
    }

    void toggleSection(ItemKind sectionKind) {
        toggle(sectionKinds, sectionKind);
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
    }

    boolean isDefault() {
        return statuses.equals(EnumSet.of(Status.FREE)) && query.isBlank() && sort == Sort.NEWEST && rarities.isEmpty() && sectionKinds.isEmpty();
    }

    void reset() {
        statuses.clear();
        statuses.add(Status.FREE);
        query = "";
        sort = Sort.NEWEST;
        rarities.clear();
        sectionKinds.clear();
    }

    private static <T> void toggle(Set<T> chosen, T value) {
        if (!chosen.remove(value)) {
            chosen.add(value);
        }
    }
}
