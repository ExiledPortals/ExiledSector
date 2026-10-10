package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SocketableStore {

    static final String DATA_KEY = "exiledSector_socketables";
    private static final String ID_PREFIX = "socketable_";
    private static final Logger LOG = Logger.getLogger(SocketableStore.class);

    private final List<Socketable> owned = new ArrayList<>();
    private long nextId = 1;
    private Map<String, Integer> hullUpgrades;
    // XStream skips transient fields, so this lookup index is rebuilt after loading instead of being written into saves
    @SuppressWarnings("java:S2065")
    private transient Map<String, Socketable> byId;

    public static SocketableStore get() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        return (SocketableStore) persistentData.computeIfAbsent(DATA_KEY, key -> new SocketableStore());
    }

    public static Socketable lookup(String socketableId) {
        if (NpcSocketables.isNpcId(socketableId)) {
            return NpcSocketables.resolve(socketableId);
        }
        return socketableId == null || Global.getSector() == null ? null : get().find(socketableId);
    }

    public Socketable find(String socketableId) {
        if (byId == null) {
            byId = new HashMap<>();
            owned.forEach(socketable -> byId.put(socketable.id(), socketable));
        }
        return byId.get(socketableId);
    }

    public List<Socketable> owned() {
        return Collections.unmodifiableList(owned);
    }

    public Socketable add(SocketableDefinition definition, long seed) {
        return add(SocketableItemData.rolled(definition, seed));
    }

    public Socketable add(SocketableItemData itemData) {
        if (itemData.definition() == null) {
            return null;
        }
        Socketable socketable = itemData.create(ID_PREFIX + nextId++);
        if (socketable == null) {
            return null;
        }
        socketable.freezeName();
        owned.add(socketable);
        if (byId != null) {
            byId.put(socketable.id(), socketable);
        }
        return socketable;
    }

    public void freezeNames() {
        owned.forEach(Socketable::freezeName);
    }

    boolean replace(Socketable replacedSocketable, Socketable replacement) {
        int ownedIndex = owned.indexOf(replacedSocketable);
        if (ownedIndex < 0) {
            return false;
        }
        owned.set(ownedIndex, replacement);
        if (byId != null) {
            byId.remove(replacedSocketable.id());
            byId.put(replacement.id(), replacement);
        }
        return true;
    }

    public boolean remove(Socketable socketable) {
        boolean removed = owned.remove(socketable);
        if (removed && byId != null) {
            byId.remove(socketable.id());
        }
        return removed;
    }

    public int upgradeCount(HullSize hullSize) {
        return hullUpgrades == null || hullSize == null ? 0 : hullUpgrades.getOrDefault(hullSize.name(), 0);
    }

    public Map<HullSize, Integer> upgradeCounts() {
        Map<HullSize, Integer> counts = new LinkedHashMap<>();
        for (HullSize hullSize : HullUpgradeData.HULL_SIZES) {
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
        SocketableItemData itemData = SocketableItemData.of(stack.getSpecialDataIfSpecial());
        if (itemData == null) {
            return 0;
        }
        if (itemData.definition() == null) {
            LOG.warn("Leaving a socketable in cargo: its definition \"" + itemData.definitionId() + "\" is not loaded");
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
