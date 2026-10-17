package exiledsector.hullitems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.apache.log4j.Logger;

import exiledsector.persistence.SaveData;
import exiledsector.skills.ShipHullSizes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HullItemStore {

    static final String DATA_KEY = "exiledSector_socketables";
    private static final String ID_PREFIX = "socketable_";
    private static final Logger LOG = Logger.getLogger(HullItemStore.class);

    private final List<HullItem> owned = new ArrayList<>();
    private long nextId = 1;
    private Map<String, Integer> hullUpgrades;
    // XStream skips transient fields, so this lookup index is rebuilt after loading instead of being written into saves
    @SuppressWarnings("java:S2065")
    private transient Map<String, HullItem> byId;
    // java:S2065: XStream skips transient fields, so this change counter restarts after loading instead of being written into saves
    @SuppressWarnings("java:S2065")
    private transient int upgradeRevision;

    public static HullItemStore get() {
        return SaveData.getOrCreate(DATA_KEY, HullItemStore::new);
    }

    public static HullItem lookup(String hullItemId) {
        if (NpcHullItems.isNpcId(hullItemId)) {
            return NpcHullItems.resolve(hullItemId);
        }
        return hullItemId == null || Global.getSector() == null ? null : get().find(hullItemId);
    }

    public HullItem find(String hullItemId) {
        if (byId == null) {
            byId = new HashMap<>();
            owned.forEach(hullItem -> byId.put(hullItem.id(), hullItem));
        }
        return byId.get(hullItemId);
    }

    public List<HullItem> owned() {
        return Collections.unmodifiableList(owned);
    }

    public HullItem add(HullItemDefinition definition, long seed) {
        return add(HullItemData.rolled(definition, seed));
    }

    public HullItem add(HullItemData itemData) {
        if (itemData.definition() == null) {
            return null;
        }
        HullItem hullItem = itemData.create(ID_PREFIX + nextId++);
        if (hullItem == null) {
            return null;
        }
        hullItem.freezeName();
        owned.add(hullItem);
        if (byId != null) {
            byId.put(hullItem.id(), hullItem);
        }
        return hullItem;
    }

    public void freezeNames() {
        owned.forEach(HullItem::freezeName);
    }

    boolean replace(HullItem replacedHullItem, HullItem replacement) {
        int ownedIndex = owned.indexOf(replacedHullItem);
        if (ownedIndex < 0) {
            return false;
        }
        owned.set(ownedIndex, replacement);
        if (byId != null) {
            byId.remove(replacedHullItem.id());
            byId.put(replacement.id(), replacement);
        }
        return true;
    }

    public boolean remove(HullItem hullItem) {
        boolean removed = owned.remove(hullItem);
        if (removed && byId != null) {
            byId.remove(hullItem.id());
        }
        return removed;
    }

    public int upgradeCount(HullSize hullSize) {
        return hullUpgrades == null || hullSize == null ? 0 : hullUpgrades.getOrDefault(hullSize.name(), 0);
    }

    public int upgradeRevision() {
        return upgradeRevision;
    }

    public Map<HullSize, Integer> upgradeCounts() {
        Map<HullSize, Integer> counts = new LinkedHashMap<>();
        for (HullSize hullSize : ShipHullSizes.ORDER) {
            int count = upgradeCount(hullSize);
            if (count > 0) {
                counts.put(hullSize, count);
            }
        }
        return counts;
    }

    public void addUpgrades(HullSize hullSize, int count) {
        if (hullSize == null || count <= 0) {
            return;
        }
        if (hullUpgrades == null) {
            hullUpgrades = new LinkedHashMap<>();
        }
        hullUpgrades.merge(hullSize.name(), count, Integer::sum);
        upgradeRevision++;
    }

    public boolean takeUpgrade(HullSize hullSize) {
        int count = upgradeCount(hullSize);
        if (count <= 0) {
            return false;
        }
        if (count == 1) {
            hullUpgrades.remove(hullSize.name());
        } else {
            hullUpgrades.put(hullSize.name(), count - 1);
        }
        upgradeRevision++;
        return true;
    }

    public int absorbFrom(CargoAPI cargo) {
        int movedCount = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            movedCount += absorbStack(cargo, stack);
        }
        return movedCount;
    }

    private int absorbStack(CargoAPI cargo, CargoStackAPI stack) {
        HullUpgradeData upgrade = HullUpgradeData.of(stack.getSpecialDataIfSpecial());
        if (upgrade != null) {
            return absorbUpgradeStack(cargo, stack, upgrade);
        }
        HullItemData itemData = HullItemData.of(stack.getSpecialDataIfSpecial());
        if (itemData == null) {
            return 0;
        }
        if (itemData.definition() == null) {
            LOG.warn("Leaving a hull item in cargo: its definition \"" + itemData.definitionId() + "\" is not loaded");
            return 0;
        }
        int stackCount = Math.round(stack.getSize());
        if (stackCount < 1) {
            return 0;
        }
        for (int i = 0; i < stackCount; i++) {
            add(itemData);
        }
        cargo.removeStack(stack);
        return stackCount;
    }

    private int absorbUpgradeStack(CargoAPI cargo, CargoStackAPI stack, HullUpgradeData upgrade) {
        int stackCount = Math.round(stack.getSize());
        if (stackCount < 1) {
            return 0;
        }
        addUpgrades(upgrade.hullSize(), stackCount);
        cargo.removeStack(stack);
        return stackCount;
    }
}
