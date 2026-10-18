package exiledsector.items;

import exiledsector.i18n.Translation;
import exiledsector.modifiers.Modifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ItemKind {

    SUBROUTINE("subroutine", ItemType.SUBROUTINE, null),
    BRIDGE("bridge", ItemType.HULL_ITEM, ShipSection.BRIDGE),
    CREW_QUARTERS("crew_quarters", ItemType.HULL_ITEM, ShipSection.CREW_QUARTERS),
    ENGINE_ROOM("engine_room", ItemType.HULL_ITEM, ShipSection.ENGINE_ROOM),
    REACTOR("reactor", ItemType.HULL_ITEM, ShipSection.REACTOR),
    ARMOR_PLATING("armor_plating", ItemType.HULL_ITEM, ShipSection.ARMOR_PLATING),
    WEAPON_MOUNT("weapon_mount", ItemType.HULL_ITEM, ShipSection.WEAPON_MOUNT),
    SHIELD_GENERATOR("shield_generator", ItemType.HULL_ITEM, ShipSection.SHIELD_GENERATOR),
    PHASE_COIL("phase_coil", ItemType.HULL_ITEM, ShipSection.PHASE_COIL),
    FLIGHT_DECK("flight_deck", ItemType.HULL_ITEM, ShipSection.FLIGHT_DECK);

    private static final List<ItemKind> SECTION_ITEM_TYPES = Arrays.stream(values()).filter(ItemKind::fitsSection).toList();

    private final String id;
    private final ItemType type;
    private final ShipSection section;

    ItemKind(String id, ItemType type, ShipSection section) {
        this.id = id;
        this.type = type;
        this.section = section;
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

    public ShipSection section() {
        return section;
    }

    public boolean fitsSection() {
        return section != null;
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
