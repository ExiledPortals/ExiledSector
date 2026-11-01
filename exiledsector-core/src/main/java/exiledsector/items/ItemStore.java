package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.ExiledSector;
import exiledsector.persistence.SaveData;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class ItemStore {

    @FunctionalInterface
    public interface StackAbsorber {
        Integer absorb(ItemStore store, CargoAPI cargo, CargoStackAPI stack);

        static <T> StackAbsorber counting(Function<SpecialItemData, T> recognise, CountedAdder<T> adder) {
            return (store, cargo, stack) -> {
                T recognised = recognise.apply(stack.getSpecialDataIfSpecial());
                if (recognised == null) {
                    return null;
                }
                int stackCount = Math.round(stack.getSize());
                if (stackCount < 1) {
                    return 0;
                }
                adder.add(store, recognised, stackCount);
                cargo.removeStack(stack);
                return stackCount;
            };
        }
    }

    @FunctionalInterface
    public interface CountedAdder<T> {
        void add(ItemStore store, T recognised, int count);
    }

    static final String DATA_KEY_NAME = "socketables";
    static final String STAR_CHART_DATA_KEY_NAME = "starCharts";
    private static final String ID_PREFIX = "socketable_";
    private static final String STAR_CHART_ID_PREFIX = "star_chart_";
    private static final Logger LOG = Logger.getLogger(ItemStore.class);
    private static final List<StackAbsorber> STACK_ABSORBERS = new CopyOnWriteArrayList<>();

    private final List<Item> owned = new ArrayList<>();
    private long nextId = 1;
    private Map<String, Integer> hullUpgrades;
    private Map<String, Integer> refitSchematics;
    private int masterSchematics;
    private boolean holdsStarCharts;
    // XStream skips transient fields, so this lookup index is rebuilt after loading instead of being written into saves
    @SuppressWarnings("java:S2065")
    private transient Map<String, Item> byId;
    // java:S2065: XStream skips transient fields, so this change counter restarts after loading instead of being written into saves
    @SuppressWarnings("java:S2065")
    private transient int shipCurrencyRevision;

    public ItemStore() {
    }

    private ItemStore(boolean holdsStarCharts) {
        this.holdsStarCharts = holdsStarCharts;
    }

    public static String dataKey() {
        return ExiledSector.key(DATA_KEY_NAME);
    }

    public static String starChartDataKey() {
        return ExiledSector.key(STAR_CHART_DATA_KEY_NAME);
    }

    public static void absorbStacksWith(StackAbsorber absorber) {
        if (!STACK_ABSORBERS.contains(absorber)) {
            STACK_ABSORBERS.add(absorber);
        }
    }

    public static ItemStore get() {
        return SaveData.getOrCreate(dataKey(), ItemStore::new);
    }

    public static ItemStore starCharts() {
        return SaveData.getOrCreate(starChartDataKey(), ItemStore::forStarCharts);
    }

    public static ItemStore starChartsIfCreated() {
        return SaveData.findOrNull(starChartDataKey(), ItemStore.class);
    }

    public static List<Item> ownedStarCharts() {
        ItemStore store = starChartsIfCreated();
        return store == null ? List.of() : store.owned();
    }

    public static boolean carriesStarCharts(CargoAPI cargo) {
        if (cargo == null) {
            return false;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            SpecialItemData special = stack.getSpecialDataIfSpecial();
            if (special != null && ItemData.STAR_CHART_ITEM_ID.equals(special.getId())) {
                return true;
            }
        }
        return false;
    }

    public static ItemStore forStarCharts() {
        return new ItemStore(true);
    }

    public boolean holdsStarCharts() {
        return holdsStarCharts;
    }

    private boolean accepts(ItemDefinition definition) {
        return definition != null && holdsStarCharts == (definition.type() == ItemType.STAR_CHART);
    }

    private String idPrefix() {
        return holdsStarCharts ? STAR_CHART_ID_PREFIX : ID_PREFIX;
    }

    public static Item lookup(String itemId) {
        ItemTypeSupport owner = ItemTypes.ownerOfItemId(itemId);
        if (owner != null) {
            return owner.resolveItem(itemId);
        }
        return itemId == null || Global.getSector() == null ? null : get().find(itemId);
    }

    public Item find(String itemId) {
        if (byId == null) {
            byId = new HashMap<>();
            owned.forEach(item -> byId.put(item.id(), item));
        }
        return byId.get(itemId);
    }

    public List<Item> owned() {
        return Collections.unmodifiableList(owned);
    }

    public Item add(ItemDefinition definition, long seed) {
        return add(ItemData.rolled(definition, seed));
    }

    public Item add(ItemData itemData) {
        if (!accepts(itemData.definition())) {
            return null;
        }
        Item item = itemData.create(idPrefix() + nextId++);
        if (item == null) {
            return null;
        }
        item.freezeName();
        owned.add(item);
        if (byId != null) {
            byId.put(item.id(), item);
        }
        return item;
    }

    public Item addMirrorOf(Item source) {
        if (source == null || !accepts(source.definition())) {
            return null;
        }
        Item copy = source.mirrorAs(idPrefix() + nextId++);
        owned.add(copy);
        if (byId != null) {
            byId.put(copy.id(), copy);
        }
        return copy;
    }

    public void freezeNames() {
        owned.forEach(Item::freezeName);
    }

    public boolean replace(Item replacedItem, Item replacement) {
        int ownedIndex = owned.indexOf(replacedItem);
        if (ownedIndex < 0) {
            return false;
        }
        owned.set(ownedIndex, replacement);
        if (byId != null) {
            byId.remove(replacedItem.id());
            byId.put(replacement.id(), replacement);
        }
        return true;
    }

    public boolean remove(Item item) {
        boolean removed = owned.remove(item);
        if (removed && byId != null) {
            byId.remove(item.id());
        }
        return removed;
    }

    public int schematicCount(RefitSchematic schematic) {
        return refitSchematics == null || schematic == null ? 0 : refitSchematics.getOrDefault(schematic.name(), 0);
    }

    public int shipCurrencyRevision() {
        return shipCurrencyRevision;
    }

    public Map<RefitSchematic, Integer> schematicCounts() {
        Map<RefitSchematic, Integer> counts = new EnumMap<>(RefitSchematic.class);
        for (RefitSchematic schematic : RefitSchematic.values()) {
            int count = schematicCount(schematic);
            if (count > 0) {
                counts.put(schematic, count);
            }
        }
        return counts;
    }

    public void addSchematics(RefitSchematic schematic, int count) {
        if (schematic == null || count <= 0) {
            return;
        }
        if (refitSchematics == null) {
            refitSchematics = new LinkedHashMap<>();
        }
        refitSchematics.merge(schematic.name(), count, Integer::sum);
        shipCurrencyRevision++;
    }

    public boolean takeSchematic(RefitSchematic schematic) {
        int count = schematicCount(schematic);
        if (count <= 0) {
            return false;
        }
        if (count == 1) {
            refitSchematics.remove(schematic.name());
        } else {
            refitSchematics.put(schematic.name(), count - 1);
        }
        shipCurrencyRevision++;
        return true;
    }

    public int masterSchematicCount() {
        return masterSchematics;
    }

    public void addMasterSchematics(int count) {
        if (count <= 0) {
            return;
        }
        masterSchematics += count;
        shipCurrencyRevision++;
    }

    public boolean takeMasterSchematic() {
        if (masterSchematics <= 0) {
            return false;
        }
        masterSchematics--;
        shipCurrencyRevision++;
        return true;
    }

    public int convertLegacyHullUpgrades() {
        if (hullUpgrades == null) {
            return 0;
        }
        int legacyCount = 0;
        for (Integer count : hullUpgrades.values()) {
            legacyCount += count == null ? 0 : Math.max(0, count);
        }
        hullUpgrades = null;
        addSchematics(RefitSchematic.STANDARD, legacyCount);
        return legacyCount;
    }

    void addLegacyHullUpgrades(String hullSizeName, int count) {
        if (hullUpgrades == null) {
            hullUpgrades = new LinkedHashMap<>();
        }
        hullUpgrades.merge(hullSizeName, count, Integer::sum);
    }

    public int absorbFrom(CargoAPI cargo) {
        int movedCount = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            movedCount += absorbStack(cargo, stack);
        }
        return movedCount;
    }

    private int absorbStack(CargoAPI cargo, CargoStackAPI stack) {
        if (holdsStarCharts) {
            return absorbItemStack(cargo, stack, ItemData.ofStarChart(stack.getSpecialDataIfSpecial()));
        }
        for (StackAbsorber absorber : STACK_ABSORBERS) {
            Integer movedCount = absorber.absorb(this, cargo, stack);
            if (movedCount != null) {
                return movedCount;
            }
        }
        return absorbItemStack(cargo, stack, ItemData.of(stack.getSpecialDataIfSpecial()));
    }

    private int absorbItemStack(CargoAPI cargo, CargoStackAPI stack, ItemData itemData) {
        if (itemData == null) {
            return 0;
        }
        if (itemData.definition() == null) {
            LOG.warn("Leaving an item in cargo: its definition \"" + itemData.definitionId() + "\" is not loaded");
            return 0;
        }
        if (!accepts(itemData.definition())) {
            LOG.warn("Leaving an item in cargo: \"" + itemData.definitionId() + "\" does not belong in this storage");
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
}
