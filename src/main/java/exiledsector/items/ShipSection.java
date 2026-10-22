package exiledsector.items;

import exiledsector.i18n.Translation;

import java.util.Arrays;
import java.util.List;

public enum ShipSection {

    BRIDGE("bridge", null),
    CREW_QUARTERS("crew_quarters", null),
    ENGINE_ROOM("engine_room", null),
    REACTOR("reactor", null),
    ARMOR_PLATING("armor_plating", null),
    WEAPON_MOUNT("weapon_mount", null),
    SHIELD_GENERATOR("shield_generator", "req_shields"),
    PHASE_COIL("phase_coil", "req_phase"),
    FLIGHT_DECK("flight_deck", "req_fighter_bays");

    private static final List<ShipSection> ALL = List.of(values());

    private final String id;
    private final String requirementTag;

    ShipSection(String id, String requirementTag) {
        this.id = id;
        this.requirementTag = requirementTag;
    }

    public static List<ShipSection> all() {
        return ALL;
    }

    public static ShipSection byIdOrNull(String id) {
        return Arrays.stream(values()).filter(section -> section.id.equals(id)).findFirst().orElse(null);
    }

    public static ShipSection of(ItemKind kind) {
        return kind == null || !kind.fitsSection() ? null : byIdOrNull(kind.id());
    }

    public String id() {
        return id;
    }

    public String requirementTag() {
        return requirementTag;
    }

    public ItemKind itemType() {
        return ItemKind.byId(id);
    }

    public String displayName() {
        return Translation.text("socket.type." + id);
    }
}
