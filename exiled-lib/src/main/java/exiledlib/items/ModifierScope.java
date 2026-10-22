package exiledlib.items;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ModifierScope {

    SUBROUTINE("subroutine", ItemType.SUBROUTINE),
    UNIQUE_SUBROUTINE("unique_subroutine", ItemType.SUBROUTINE),
    DOCTRINE("doctrine", ItemType.SUBROUTINE),
    HULL_ITEM("hull_item", ItemType.HULL_ITEM),
    STAR_CHART("star_chart", ItemType.STAR_CHART),
    WORMHOLE_ANCHOR("wormhole_anchor", ItemType.WORMHOLE_ANCHOR);

    private final String id;
    private final ItemType itemType;

    ModifierScope(String id, ItemType itemType) {
        this.id = id;
        this.itemType = itemType;
    }

    public String id() {
        return id;
    }

    public ItemType itemType() {
        return itemType;
    }

    public static ModifierScope byIdOrNull(String id) {
        for (ModifierScope scope : values()) {
            if (scope.id.equals(id)) {
                return scope;
            }
        }
        return null;
    }

    public static ModifierScope of(ItemType type) {
        return type == null ? null : byIdOrNull(type.id());
    }

    public static String ids() {
        return Arrays.stream(values()).map(ModifierScope::id).collect(Collectors.joining(", "));
    }
}
