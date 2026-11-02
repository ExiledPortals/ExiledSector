package exiledsector.items;

import java.util.List;

public final class WormholeTechItems {

    public static final String BLUEPRINT = "exiledSector_wormhole_blueprint";
    public static final String UPGRADE_ARRAY = "exiledSector_gateway_upgrade_advanced";
    public static final String UPGRADE_NEXUS = "exiledSector_gateway_upgrade_perfected";
    public static final List<String> ALL = List.of(BLUEPRINT, UPGRADE_ARRAY, UPGRADE_NEXUS);

    private WormholeTechItems() {
    }

    public static void localise() {
        ALL.forEach(itemId -> MaterialItems.localise(itemId, "wormholeTech."));
    }
}
