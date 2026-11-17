package exiledsector.items.custody;

import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.skills.ShipSkillData;

import java.util.List;
import java.util.Map;

public interface ShipCustody {

    boolean holdsItems(ShipSkillData shipData);

    default boolean holdsItems(String shipId, ShipSkillData shipData) {
        return holdsItems(shipData);
    }

    List<? extends CargoRefund> release(ShipSkillData shipData, boolean destroyed, ItemStore store);

    default List<? extends CargoRefund> release(String shipId, ShipSkillData shipData, boolean destroyed, ItemStore store) {
        return release(shipData, destroyed, store);
    }

    boolean unsocketIfMisfit(ShipSkillData shipData, Item item);

    default void forgetShip(String shipId) {
    }

    default List<Item> returnMisfits(ShipSkillData shipData, ItemStore store) {
        return List.of();
    }

    default void enforceLimits(Map<String, ShipSkillData> shipDataById, ItemStore store) {
    }
}
