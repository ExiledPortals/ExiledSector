package exiledsector.items;

import exiledlib.i18n.Translation;
import exiledsector.modifiers.Modifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ItemKind {

    SUBROUTINE("subroutine", ItemType.SUBROUTINE),
    BRIDGE("bridge", ItemType.HULL_ITEM),
    CREW_QUARTERS("crew_quarters", ItemType.HULL_ITEM),
    ENGINE_ROOM("engine_room", ItemType.HULL_ITEM),
    REACTOR("reactor", ItemType.HULL_ITEM),
    ARMOR_PLATING("armor_plating", ItemType.HULL_ITEM),
    WEAPON_MOUNT("weapon_mount", ItemType.HULL_ITEM),
    SHIELD_GENERATOR("shield_generator", ItemType.HULL_ITEM),
    PHASE_COIL("phase_coil", ItemType.HULL_ITEM),
    FLIGHT_DECK("flight_deck", ItemType.HULL_ITEM);

    private static final List<ItemKind> SECTION_ITEM_TYPES = Arrays.stream(values()).filter(ItemKind::fitsSection).toList();

    private final String id;
    private final ItemType type;

    ItemKind(String id, ItemType type) {
        this.id = id;
        this.type = type;
    }

    public ItemType type() {
        return type;
    }

    public static ItemKind byId(String id) {
        for (ItemKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown item kind \"" + id + "\"; expected one of "
                + Arrays.stream(values()).map(ItemKind::id).collect(Collectors.joining(", ")));
    }

    public static ItemKind byIdOrNull(String id) {
        for (ItemKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }

    public static List<ItemKind> sectionItemTypes() {
        return SECTION_ITEM_TYPES;
    }

    public String id() {
        return id;
    }

    public boolean fitsSection() {
        return type == ItemType.HULL_ITEM;
    }

    public List<Modifier> implicits() {
        return ItemImplicits.forType(this);
    }

    public String displayName() {
        return Translation.text("socket.type." + id);
    }

    public String noun() {
        return Translation.text("socketable.noun." + id);
    }
}
