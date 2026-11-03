package exiledsector.ui.socket;

import exiledsector.items.ItemKind;
import exiledsector.items.ItemRarity;

import java.util.EnumSet;
import java.util.Set;

public final class ItemStorageFilter {

    public enum Status {ALL, FREE, INSTALLED}

    public enum Sort {AGE, RARITY, VALUE}

    static final ItemStorageFilter SESSION = new ItemStorageFilter();

    Status status = Status.FREE;
    String query = "";
    Sort sort = Sort.AGE;
    boolean sortReversed;
    final Set<ItemRarity> rarities = EnumSet.noneOf(ItemRarity.class);
    final Set<ItemKind> sectionKinds = EnumSet.noneOf(ItemKind.class);

    void setStatus(Status value) {
        status = value;
    }

    void pickSort(Sort chosen) {
        if (chosen == sort) {
            sortReversed = !sortReversed;
            return;
        }
        sort = chosen;
        sortReversed = false;
    }

    void toggleRarity(ItemRarity rarity) {
        if (!rarities.remove(rarity)) {
            rarities.add(rarity);
        }
    }

    void toggleSection(ItemKind sectionKind) {
        if (!sectionKinds.remove(sectionKind)) {
            sectionKinds.add(sectionKind);
        }
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
    }
}
