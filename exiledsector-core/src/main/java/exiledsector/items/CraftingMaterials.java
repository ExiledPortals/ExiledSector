package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledlib.ModSettings;
import exiledsector.persistence.SaveData;

import java.util.HashMap;
import java.util.Map;

public final class CraftingMaterials {

    public static final String STORE_FIELD_ID = "exiledSector_storeSocketMaterials";
    public static final boolean DEFAULT_STORE = false;
    static final String STORED_KEY = "exiledSector_storedSocketMaterials";

    private final CargoAPI cargo;
    private final Map<String, Integer> storedCounts;
    private final boolean storing;

    CraftingMaterials(CargoAPI cargo, Map<String, Integer> storedCounts, boolean storing) {
        this.cargo = cargo;
        this.storedCounts = storedCounts;
        this.storing = storing;
    }

    public static CraftingMaterials forPlayer(CargoAPI cargo) {
        return new CraftingMaterials(cargo, storedCounts(), ModSettings.booleanOr(STORE_FIELD_ID, DEFAULT_STORE));
    }

    public static CraftingMaterials storedForTests(Map<String, Integer> storedCounts) {
        return new CraftingMaterials(null, storedCounts, true);
    }

    public static CraftingMaterials cargoOnly(CargoAPI cargo) {
        return new CraftingMaterials(cargo, new HashMap<>(), false);
    }

    private static Map<String, Integer> storedCounts() {
        return SaveData.getOrCreate(STORED_KEY, HashMap::new);
    }

    public boolean isStoring() {
        return storing;
    }

    public int count(String itemId) {
        return stored(itemId) + inCargo(itemId);
    }

    public int stored(String itemId) {
        return storedCounts.getOrDefault(itemId, 0);
    }

    public boolean take(String itemId, int quantity) {
        if (quantity <= 0) {
            return true;
        }
        if (count(itemId) < quantity) {
            return false;
        }
        int fromStore = Math.min(stored(itemId), quantity);
        setStored(itemId, stored(itemId) - fromStore);
        if (quantity > fromStore && cargo != null) {
            MaterialItems.remove(cargo, itemId, quantity - fromStore);
        }
        return true;
    }

    public void add(String itemId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        if (storing || cargo == null) {
            setStored(itemId, stored(itemId) + quantity);
        } else {
            MaterialItems.add(cargo, itemId, quantity);
        }
    }

    public int settle() {
        if (cargo == null) {
            return 0;
        }
        int movedCount = 0;
        for (String itemId : MaterialItems.ids()) {
            if (storing) {
                int carried = inCargo(itemId);
                if (carried > 0) {
                    MaterialItems.remove(cargo, itemId, carried);
                    setStored(itemId, stored(itemId) + carried);
                    movedCount += carried;
                }
            } else {
                int kept = stored(itemId);
                if (kept > 0) {
                    setStored(itemId, 0);
                    MaterialItems.add(cargo, itemId, kept);
                    movedCount += kept;
                }
            }
        }
        return movedCount;
    }

    private int inCargo(String itemId) {
        return MaterialItems.count(cargo, itemId);
    }

    private void setStored(String itemId, int quantity) {
        if (quantity > 0) {
            storedCounts.put(itemId, quantity);
        } else {
            storedCounts.remove(itemId);
        }
    }
}
