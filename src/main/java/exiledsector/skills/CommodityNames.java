package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

public final class CommodityNames {

    private CommodityNames() {
    }

    public static String displayName(String commodityId) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(commodityId);
        return spec != null ? spec.getName() : commodityId;
    }
}
