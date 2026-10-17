package exiledsector.hullitems;

import exiledsector.i18n.Translation;
import exiledsector.modifiers.Modifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum HullItemType {

    SUBROUTINE("subroutine", null),
    BRIDGE("bridge", ShipSection.BRIDGE),
    CREW_QUARTERS("crew_quarters", ShipSection.CREW_QUARTERS),
    ENGINE_ROOM("engine_room", ShipSection.ENGINE_ROOM),
    REACTOR("reactor", ShipSection.REACTOR),
    ARMOR_PLATING("armor_plating", ShipSection.ARMOR_PLATING),
    WEAPON_MOUNT("weapon_mount", ShipSection.WEAPON_MOUNT),
    SHIELD_GENERATOR("shield_generator", ShipSection.SHIELD_GENERATOR),
    PHASE_COIL("phase_coil", ShipSection.PHASE_COIL),
    FLIGHT_DECK("flight_deck", ShipSection.FLIGHT_DECK);

    private static final List<HullItemType> SECTION_ITEM_TYPES = Arrays.stream(values()).filter(HullItemType::fitsSection).toList();

    private final String id;
    private final ShipSection section;

    HullItemType(String id, ShipSection section) {
        this.id = id;
        this.section = section;
    }

    public static HullItemType byId(String id) {
        for (HullItemType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown hull item type \"" + id + "\"; expected one of "
                + Arrays.stream(values()).map(HullItemType::id).collect(Collectors.joining(", ")));
    }

    public static HullItemType byIdOrNull(String id) {
        for (HullItemType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    public static List<HullItemType> sectionItemTypes() {
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
        return HullItemImplicits.forType(this);
    }

    public String displayName() {
        return Translation.text("socket.type." + id);
    }

    public String noun() {
        return Translation.text("socketable.noun." + id);
    }
}
