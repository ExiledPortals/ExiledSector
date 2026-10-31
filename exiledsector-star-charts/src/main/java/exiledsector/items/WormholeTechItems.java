package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;

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

    public static String displayName(String itemId) {
        return MaterialItems.displayName(itemId);
    }

    public static SpecialItemData data(String itemId) {
        return MaterialItems.data(itemId);
    }

    public static void addTo(CargoAPI cargo, String itemId) {
        MaterialItems.add(cargo, itemId, 1);
    }
}
