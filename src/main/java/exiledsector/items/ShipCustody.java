package exiledsector.items;

import exiledsector.skills.ShipSkillData;

import java.util.List;
import java.util.Map;

public interface ShipCustody {

    boolean holdsItems(ShipSkillData shipData);

    List<? extends CargoRefund> release(ShipSkillData shipData, boolean destroyed, ItemStore store);

    boolean unsocketIfMisfit(ShipSkillData shipData, Item item);

    default void forgetShip(String shipId) {
    }

    default List<Item> returnMisfits(ShipSkillData shipData, ItemStore store) {
        return List.of();
    }

    default void enforceLimits(Map<String, ShipSkillData> shipDataById, ItemStore store) {
    }
}
