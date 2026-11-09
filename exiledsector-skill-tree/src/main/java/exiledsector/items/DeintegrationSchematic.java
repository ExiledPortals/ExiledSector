package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.i18n.Translation;
import exiledsector.skills.IntegratedNodes;
import exiledsector.skills.NodeIntegration;
import exiledsector.skills.ShipSkillData;

public enum DeintegrationSchematic implements ShipSchematic {

    INSTANCE;

    public static final String ITEM_ID = "exiledSector_deintegration_schematic";
    public static final String ICON_PATH = "graphics/icons/hull_upgrades/deintegration_schematic.png";

    public static int held(CargoAPI cargo) {
        return cargo == null ? 0 : (int) cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, INSTANCE.toSpecialItem());
    }

    public static int refundFor(ShipSkillData shipData) {
        int integratedCount = shipData == null ? 0 : shipData.getIntegratedNodeIds().size();
        return IntegratedNodes.costOfLast(integratedCount, integratedCount);
    }

    public static int useOn(CargoAPI cargo, ShipSkillData shipData) {
        int refund = refundFor(shipData);
        if (refund < 1 || held(cargo) < 1) {
            return 0;
        }
        cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, INSTANCE.toSpecialItem(), 1f);
        shipData.clearIntegrations();
        NodeIntegration.refundStoryPoints(refund);
        return refund;
    }

    @Override
    public SpecialItemData toSpecialItem() {
        return new SpecialItemData(ITEM_ID, null);
    }

    @Override
    public String displayName() {
        return Translation.text("deintegrationSchematic.name");
    }
}
