package exiledsector.items;

import java.util.List;

public final class WormholeTechItems {

    public static final String BLUEPRINT = "exiledSector_wormhole_blueprint";
    public static final String UPGRADE_ADVANCED = "exiledSector_gateway_upgrade_advanced";
    public static final String UPGRADE_PERFECTED = "exiledSector_gateway_upgrade_perfected";
    public static final List<String> ALL = List.of(BLUEPRINT, UPGRADE_ADVANCED, UPGRADE_PERFECTED);

    private WormholeTechItems() {
    }

    public static void localise() {
        ALL.forEach(itemId -> MaterialItems.localise(itemId, "gatewayItem."));
    }
}
