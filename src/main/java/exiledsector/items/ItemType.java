package exiledsector.items;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ItemType {

    SUBROUTINE("subroutine"),
    HULL_ITEM("hull_item");

    private final String id;

    ItemType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ItemType byIdOrNull(String id) {
        for (ItemType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    public static String ids() {
        return Arrays.stream(values()).map(ItemType::id).collect(Collectors.joining(", "));
    }
}
