package exiledlib.items;

public enum ItemNature {

    HARDWARE,
    CREW,
    SOFTWARE;

    public static ItemNature of(ItemKind kind) {
        if (kind == null) {
            return HARDWARE;
        }
        return switch (kind) {
            case SUBROUTINE -> SOFTWARE;
            case BRIDGE, CREW_QUARTERS, FLIGHT_DECK -> CREW;
            default -> HARDWARE;
        };
    }
}
