package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import exiledsector.i18n.Translation;

import java.util.List;

public final class WormholeTechItems {

    public static final String BLUEPRINT = "exiledSector_wormhole_blueprint";
    public static final String UPGRADE_ADVANCED = "exiledSector_gateway_upgrade_advanced";
    public static final String UPGRADE_PERFECTED = "exiledSector_gateway_upgrade_perfected";
    public static final List<String> ALL = List.of(BLUEPRINT, UPGRADE_ADVANCED, UPGRADE_PERFECTED);

    private WormholeTechItems() {
    }

    public static void localise() {
        for (String itemId : ALL) {
            SpecialItemSpecAPI spec = MaterialItems.spec(itemId);
            if (spec != null) {
                spec.setName(Translation.data("gatewayItem." + itemId + ".name", spec.getName()));
                spec.setDesc(Translation.data("gatewayItem." + itemId + ".description", spec.getDesc()));
            }
        }
    }

    public static String displayName(String itemId) {
        return MaterialItems.displayName(itemId);
    }

    public static SpecialItemData data(String itemId) {
        return new SpecialItemData(itemId, null);
    }

    public static void addTo(CargoAPI cargo, String itemId) {
        cargo.addSpecial(data(itemId), 1f);
    }
}
