package exiledsector.items;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum SubroutineKind {

    VOLUME(true, true, SubroutineKind.UNLIMITED_PER_SHIP),
    DOCTRINE(false, false, 1),
    BYPASS(true, true, 1),
    UNIQUE(false, false, SubroutineKind.UNLIMITED_PER_SHIP);

    public static final int UNLIMITED_PER_SHIP = Integer.MAX_VALUE;

    private final boolean bornCorrupted;
    private final boolean rollsNode;
    private final int socketLimitPerShip;

    SubroutineKind(boolean bornCorrupted, boolean rollsNode, int socketLimitPerShip) {
        this.bornCorrupted = bornCorrupted;
        this.rollsNode = rollsNode;
        this.socketLimitPerShip = socketLimitPerShip;
    }

    public String id() {
        return name();
    }

    public boolean bornCorrupted() {
        return bornCorrupted;
    }

    public boolean rollsNode() {
        return rollsNode;
    }

    public int socketLimitPerShip() {
        return socketLimitPerShip;
    }

    public boolean limitsSocketsPerShip() {
        return socketLimitPerShip != UNLIMITED_PER_SHIP;
    }

    public static SubroutineKind byIdOrNull(String id) {
        if (id == null) {
            return null;
        }
        String normalisedId = id.trim().toUpperCase(Locale.ROOT);
        for (SubroutineKind kind : values()) {
            if (kind.name().equals(normalisedId)) {
                return kind;
            }
        }
        return null;
    }

    public static String ids() {
        return Arrays.stream(values()).map(SubroutineKind::id).collect(Collectors.joining(", "));
    }
}
