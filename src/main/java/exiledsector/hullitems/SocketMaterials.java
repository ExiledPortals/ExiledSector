package exiledsector.hullitems;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.ModSettings;
import exiledsector.persistence.SaveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SocketMaterials {

    public static final String STORE_FIELD_ID = "exiledSector_storeSocketMaterials";
    public static final boolean DEFAULT_STORE = false;
    static final String STORED_KEY = "exiledSector_storedSocketMaterials";

    private final CargoAPI cargo;
    private final Map<String, Integer> storedCounts;
    private final boolean storing;

    SocketMaterials(CargoAPI cargo, Map<String, Integer> storedCounts, boolean storing) {
        this.cargo = cargo;
        this.storedCounts = storedCounts;
        this.storing = storing;
    }

    public static SocketMaterials forPlayer(CargoAPI cargo) {
        return new SocketMaterials(cargo, storedCounts(), ModSettings.booleanOr(STORE_FIELD_ID, DEFAULT_STORE));
    }

    public static SocketMaterials cargoOnly(CargoAPI cargo) {
        return new SocketMaterials(cargo, new HashMap<>(), false);
    }

    private static Map<String, Integer> storedCounts() {
        return SaveData.getOrCreate(STORED_KEY, HashMap::new);
    }

    public static List<String> commodityIds() {
        List<String> commodityIds = new ArrayList<>();
        commodityIds.add(HullItemDisassembly.PARTS_COMMODITY_ID);
        for (SocketCurrency currency : SocketCurrency.values()) {
            commodityIds.add(currency.commodityId());
        }
        return commodityIds;
    }

    public boolean isStoring() {
        return storing;
    }

    public int count(String commodityId) {
        return stored(commodityId) + inCargo(commodityId);
    }

    public int stored(String commodityId) {
        return storedCounts.getOrDefault(commodityId, 0);
    }

    public boolean take(String commodityId, int quantity) {
        if (quantity <= 0) {
            return true;
        }
        if (count(commodityId) < quantity) {
            return false;
        }
        int fromStore = Math.min(stored(commodityId), quantity);
        setStored(commodityId, stored(commodityId) - fromStore);
        if (quantity > fromStore && cargo != null) {
            cargo.removeCommodity(commodityId, quantity - fromStore);
        }
        return true;
    }

    public void add(String commodityId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        if (storing || cargo == null) {
            setStored(commodityId, stored(commodityId) + quantity);
        } else {
            cargo.addCommodity(commodityId, quantity);
        }
    }

    public int settle() {
        if (cargo == null) {
            return 0;
        }
        int movedCount = 0;
        for (String commodityId : commodityIds()) {
            if (storing) {
                int carried = inCargo(commodityId);
                if (carried > 0) {
                    cargo.removeCommodity(commodityId, carried);
                    setStored(commodityId, stored(commodityId) + carried);
                    movedCount += carried;
                }
            } else {
                int kept = stored(commodityId);
                if (kept > 0) {
                    setStored(commodityId, 0);
                    cargo.addCommodity(commodityId, kept);
                    movedCount += kept;
                }
            }
        }
        return movedCount;
    }

    private int inCargo(String commodityId) {
        return cargo == null ? 0 : Math.round(cargo.getCommodityQuantity(commodityId));
    }

    private void setStored(String commodityId, int quantity) {
        if (quantity > 0) {
            storedCounts.put(commodityId, quantity);
        } else {
            storedCounts.remove(commodityId);
        }
    }
}
