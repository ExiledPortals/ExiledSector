package exiledsector.socketables;

import exiledsector.i18n.Translation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum SocketType {

    SUBROUTINE("subroutine", null, false),
    BRIDGE("bridge", null, true),
    CREW_QUARTERS("crew_quarters", null, true),
    ENGINE_ROOM("engine_room", null, true),
    REACTOR("reactor", null, true),
    WEAPON_MOUNT("weapon_mount", null, true),
    SHIELD_GENERATOR("shield_generator", "req_shields", true),
    PHASE_COIL("phase_coil", "req_phase", true),
    FLIGHT_DECK("flight_deck", "req_fighter_bays", true);

    private static final List<SocketType> FRAMEWORK_TYPES = Arrays.stream(values()).filter(SocketType::isFramework).toList();

    private final String id;
    private final String requirementTag;
    private final boolean framework;

    SocketType(String id, String requirementTag, boolean framework) {
        this.id = id;
        this.requirementTag = requirementTag;
        this.framework = framework;
    }

    public static SocketType byId(String id) {
        for (SocketType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown socket type \"" + id + "\"; expected one of "
                + Arrays.stream(values()).map(SocketType::id).collect(Collectors.joining(", ")));
    }

    public static SocketType byIdOrNull(String id) {
        for (SocketType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    public static List<SocketType> frameworkTypes() {
        return FRAMEWORK_TYPES;
    }

    public String id() {
        return id;
    }

    public String requirementTag() {
        return requirementTag;
    }

    public boolean isFramework() {
        return framework;
    }

    public String displayName() {
        return Translation.text("socket.type." + id);
    }

    public String noun() {
        return Translation.text("socketable.noun." + id);
    }
}
