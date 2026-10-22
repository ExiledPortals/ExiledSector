package exiledsector.items;

import exiledlib.items.ItemDefinition;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum SubroutineKind {

    MODSPEC_CACHE(true, true, SubroutineKind.UNLIMITED_PER_SHIP, false, false),
    DOCTRINE(false, false, 1, false, true),
    BYPASS(true, true, 1, false, false),
    UNIQUE(false, false, SubroutineKind.UNLIMITED_PER_SHIP, true, true);

    public static final int UNLIMITED_PER_SHIP = Integer.MAX_VALUE;

    private final boolean bornCorrupted;
    private final boolean rollsNode;
    private final int socketLimitPerShip;
    private final boolean inGenericPools;
    private final boolean uniqueRarity;

    SubroutineKind(boolean bornCorrupted, boolean rollsNode, int socketLimitPerShip, boolean inGenericPools, boolean uniqueRarity) {
        this.bornCorrupted = bornCorrupted;
        this.rollsNode = rollsNode;
        this.socketLimitPerShip = socketLimitPerShip;
        this.inGenericPools = inGenericPools;
        this.uniqueRarity = uniqueRarity;
    }

    public boolean uniqueRarity() {
        return uniqueRarity;
    }

    public boolean inGenericPools() {
        return inGenericPools;
    }

    public static boolean inGenericPools(ItemDefinition definition) {
        SubroutineKind kind = of(definition);
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

    public static SubroutineKind of(ItemDefinition definition) {
        return definition != null && definition.subtype() instanceof SubroutineKind kind ? kind : null;
    }

    public static boolean isUnique(ItemDefinition definition) {
        return of(definition) == UNIQUE;
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
