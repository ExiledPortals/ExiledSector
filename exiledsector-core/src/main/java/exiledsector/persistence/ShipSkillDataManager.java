package exiledsector.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import exiledlib.persistence.SaveData;
import exiledsector.skills.ShipSkillData;

public class ShipSkillDataManager {

    private static final String DATA_KEY = "exiledSector_shipSkillData";

    private ShipSkillDataManager() {
    }

    private static Map<String, ShipSkillData> getStore() {
        return SaveData.getOrCreate(DATA_KEY, HashMap::new);
    }

    public static ShipSkillData get(String shipId) {
        return getStore().computeIfAbsent(shipId, key -> new ShipSkillData());
    }

    public static Map<String, ShipSkillData> all() {
        return Collections.unmodifiableMap(getStore());
    }

    public static ShipSkillData find(String shipId) {
        return getStore().get(shipId);
    }

    public static boolean hasProgress(String shipId) {
        ShipSkillData shipData = find(shipId);
        return shipData != null && !shipData.isBlank();
    }

    public static void remove(String shipId) {
        getStore().remove(shipId);
    }

    public static void removeBlankRecords() {
        getStore().values().removeIf(ShipSkillData::isBlank);
    }

    public static void put(String shipId, ShipSkillData shipData) {
        getStore().put(shipId, shipData);
    }
}
