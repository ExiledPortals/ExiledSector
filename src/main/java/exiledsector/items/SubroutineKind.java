package exiledsector.items;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum SubroutineKind {

    VOLUME(true, true, SubroutineKind.UNLIMITED_PER_SHIP, false),
    DOCTRINE(false, false, 1, false),
    BYPASS(true, true, 1, false),
    UNIQUE(false, false, SubroutineKind.UNLIMITED_PER_SHIP, true);

    public static final int UNLIMITED_PER_SHIP = Integer.MAX_VALUE;

    private final boolean bornCorrupted;
    private final boolean rollsNode;
    private final int socketLimitPerShip;
    private final boolean inGenericPools;

    SubroutineKind(boolean bornCorrupted, boolean rollsNode, int socketLimitPerShip, boolean inGenericPools) {
        this.bornCorrupted = bornCorrupted;
        this.rollsNode = rollsNode;
        this.socketLimitPerShip = socketLimitPerShip;
        this.inGenericPools = inGenericPools;
    }

    public boolean inGenericPools() {
        return inGenericPools;
    }

    public static boolean inGenericPools(ItemDefinition definition) {
        SubroutineKind kind = definition == null ? null : definition.subroutineKind();
        return kind == null || kind.inGenericPools();
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
