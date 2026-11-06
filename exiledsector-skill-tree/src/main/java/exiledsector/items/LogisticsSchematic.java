package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipSkillData;

public enum LogisticsSchematic implements ShipSchematic {

    INSTANCE;

    public static final String ITEM_ID = "exiledSector_logistics_schematic";
    public static final String ICON_PATH = "graphics/icons/hull_upgrades/logistics_schematic.png";

    public static boolean is(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId());
    }

    public static int held(CargoAPI cargo) {
        return cargo == null ? 0 : (int) cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, INSTANCE.toSpecialItem());
    }

    public static boolean useOn(CargoAPI cargo, ShipSkillData shipData) {
        if (shipData == null || held(cargo) < 1) {
            return false;
        }
        cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, INSTANCE.toSpecialItem(), 1f);
        shipData.addLogisticsSlot();
        return true;
    }

    @Override
    public SpecialItemData toSpecialItem() {
        return new SpecialItemData(ITEM_ID, null);
    }

    @Override
    public String displayName() {
        return Translation.text("logisticsSchematic.name");
    }
}
