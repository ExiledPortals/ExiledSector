package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SocketableStore {

    static final String DATA_KEY = "exiledSector_socketables";
    private static final String ID_PREFIX = "socketable_";
    private static final String FRAMEWORK_ID_PREFIX = "framework_";
    private static final Logger LOG = Logger.getLogger(SocketableStore.class);
    private static final Map<String, HullFramework> NPC_FRAMEWORK_PREVIEWS = new ConcurrentHashMap<>();
    private static final int MAX_NPC_FRAMEWORK_PREVIEWS = 1024;

    private final List<Socketable> owned = new ArrayList<>();
    private long nextId = 1;
    private List<HullFramework> frameworks;
    private long lastFrameworkId;
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

    public static HullFramework lookupFramework(String frameworkId) {
        if (HullFrameworkData.isNpcId(frameworkId)) {
            HullFramework cachedPreview = NPC_FRAMEWORK_PREVIEWS.get(frameworkId);
            if (cachedPreview != null) {
                return cachedPreview;
            }
            HullFrameworkData frameworkData = HullFrameworkData.ofNpcId(frameworkId);
            if (frameworkData == null) {
                return null;
            }
            if (NPC_FRAMEWORK_PREVIEWS.size() >= MAX_NPC_FRAMEWORK_PREVIEWS) {
                NPC_FRAMEWORK_PREVIEWS.clear();
            }
            HullFramework preview = frameworkData.preview();
            NPC_FRAMEWORK_PREVIEWS.put(frameworkId, preview);
            return preview;
        }
        return frameworkId == null || Global.getSector() == null ? null : get().findFramework(frameworkId);
    }

    public static void clearNpcFrameworkPreviews() {
        NPC_FRAMEWORK_PREVIEWS.clear();
    }

    public List<HullFramework> frameworks() {
        return frameworks == null ? List.of() : Collections.unmodifiableList(frameworks);
    }

    public HullFramework findFramework(String frameworkId) {
        if (frameworks == null || frameworkId == null) {
            return null;
        }
        for (HullFramework framework : frameworks) {
            if (frameworkId.equals(framework.id())) {
                return framework;
            }
        }
        return null;
    }

    public HullFramework addFramework(HullFrameworkData frameworkData) {
        if (frameworkData == null) {
            return null;
        }
        if (frameworks == null) {
            frameworks = new ArrayList<>();
        }
        HullFramework framework = new HullFramework(FRAMEWORK_ID_PREFIX + ++lastFrameworkId, frameworkData);
        frameworks.add(framework);
        return framework;
    }

    public boolean removeFramework(HullFramework framework) {
        return frameworks != null && frameworks.remove(framework);
    }

    public int absorbFrom(CargoAPI cargo) {
        int movedCount = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            movedCount += absorbStack(cargo, stack);
        }
        return movedCount;
    }

    private int absorbStack(CargoAPI cargo, CargoStackAPI stack) {
        HullFrameworkData frameworkData = HullFrameworkData.of(stack.getSpecialDataIfSpecial());
        if (frameworkData != null) {
            return absorbFrameworkStack(cargo, stack, frameworkData);
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

    private int absorbFrameworkStack(CargoAPI cargo, CargoStackAPI stack, HullFrameworkData frameworkData) {
        int stackCount = Math.round(stack.getSize());
        if (stackCount < 1) {
            return 0;
        }
        for (int i = 0; i < stackCount; i++) {
            addFramework(frameworkData);
        }
        cargo.removeStack(stack);
        return stackCount;
    }
}
