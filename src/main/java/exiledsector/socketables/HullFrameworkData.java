package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record HullFrameworkData(HullSize hullSize, SocketableRarity rarity, List<SocketType> socketTypes, long seed) {

    public static final String ITEM_ID = "exiledSector_hull_framework";
    public static final String NPC_PREFIX = "npcfw:";
    static final String ICON = "graphics/icons/cargo/blueprint_shiphull.png";
    private static final String ICON_FOLDER = "graphics/icons/frameworks/framework_";
    private static final String FIELD_SEPARATOR = "/";
    private static final String TYPE_SEPARATOR = "+";
    private static final Set<HullSize> SHIP_HULL_SIZES = Set.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    public HullFrameworkData {
        socketTypes = List.copyOf(socketTypes);
    }

    public static HullFrameworkData parse(String encoded) {
        if (encoded == null) {
            return null;
        }
        String[] fields = encoded.split(FIELD_SEPARATOR, -1);
        if (fields.length != 4) {
            return null;
        }
        try {
            HullSize hullSize = HullSize.valueOf(fields[0]);
            SocketableRarity rarity = SocketableRarity.valueOf(fields[1]);
            Set<SocketType> socketTypes = new LinkedHashSet<>();
            for (String socketTypeId : fields[2].split("\\" + TYPE_SEPARATOR)) {
                SocketType socketType = SocketType.byIdOrNull(socketTypeId);
                if (socketType != null && socketType.isFramework()) {
                    socketTypes.add(socketType);
                }
            }
            if (!SHIP_HULL_SIZES.contains(hullSize) || socketTypes.isEmpty()) {
                return null;
            }
            return new HullFrameworkData(hullSize, rarity, new ArrayList<>(socketTypes), Long.parseLong(fields[3]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static HullFrameworkData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public static boolean isNpcId(String frameworkId) {
        return frameworkId != null && frameworkId.startsWith(NPC_PREFIX);
    }

    public static HullFrameworkData ofNpcId(String frameworkId) {
        return isNpcId(frameworkId) ? parse(frameworkId.substring(NPC_PREFIX.length())) : null;
    }

    public String encode() {
        return hullSize.name() + FIELD_SEPARATOR + rarity.name() + FIELD_SEPARATOR
                + socketTypes.stream().map(SocketType::id).collect(Collectors.joining(TYPE_SEPARATOR)) + FIELD_SEPARATOR + seed;
    }

    public String npcId() {
        return NPC_PREFIX + encode();
    }

    public SpecialItemData toSpecialItem() {
        return new SpecialItemData(ITEM_ID, encode());
    }

    public String iconPath() {
        return iconPath(hullSize, rarity);
    }

    static String iconPath(HullSize hullSize, SocketableRarity rarity) {
        if (hullSize == null || rarity == null) {
            return ICON;
        }
        String sizeName = hullSize == HullSize.CAPITAL_SHIP ? "capital" : hullSize.name().toLowerCase(Locale.ROOT);
        return ICON_FOLDER + sizeName + "_" + rarity.name().toLowerCase(Locale.ROOT) + ".png";
    }

    public HullFramework preview() {
        return new HullFramework(npcId(), this);
    }
}
