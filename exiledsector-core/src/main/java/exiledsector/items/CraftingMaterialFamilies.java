package exiledsector.items;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class CraftingMaterialFamilies {

    private static final List<CraftingMaterialFamily> BUILT_IN = List.of(Nanoswarm.values());
    private static final AtomicReference<List<CraftingMaterialFamily>> REGISTERED = new AtomicReference<>(BUILT_IN);
    private static final AtomicReference<Set<Extension>> EXTENSIONS = new AtomicReference<>(Set.of());

    private record Extension(CraftingMaterialFamily craftingItem, ItemType type) {
    }

    private CraftingMaterialFamilies() {
    }

    public static List<CraftingMaterialFamily> all() {
        return REGISTERED.get();
    }

    public static List<CraftingMaterialFamily> forType(ItemType type) {
        return all().stream().filter(craftingItem -> appliesTo(craftingItem, type)).toList();
    }

    public static boolean appliesTo(CraftingMaterialFamily craftingItem, ItemType type) {
        if (craftingItem == null || type == null) {
            return false;
        }
        return craftingItem.appliesTo(type) || EXTENSIONS.get().contains(new Extension(craftingItem, type));
    }

    public static boolean extend(CraftingMaterialFamily craftingItem, ItemType type) {
        if (craftingItem == null || type == null || !all().contains(craftingItem)) {
            throw new IllegalArgumentException("only a registered crafting item can be extended to another item type");
        }
        if (craftingItem.appliesTo(type)) {
            return false;
        }
        Set<Extension> previous;
        Set<Extension> updated;
        do {
            previous = EXTENSIONS.get();
            if (previous.contains(new Extension(craftingItem, type))) {
                return false;
            }
            Set<Extension> extended = new HashSet<>(previous);
            extended.add(new Extension(craftingItem, type));
            updated = Set.copyOf(extended);
        } while (!EXTENSIONS.compareAndSet(previous, updated));
        return true;
    }

    public static boolean retract(CraftingMaterialFamily craftingItem, ItemType type) {
        Set<Extension> previous;
        Set<Extension> updated;
        do {
            previous = EXTENSIONS.get();
            if (!previous.contains(new Extension(craftingItem, type))) {
                return false;
            }
            Set<Extension> remaining = new HashSet<>(previous);
            remaining.remove(new Extension(craftingItem, type));
            updated = Set.copyOf(remaining);
        } while (!EXTENSIONS.compareAndSet(previous, updated));
        return true;
    }

    public static CraftingMaterialFamily byItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (CraftingMaterialFamily craftingItem : all()) {
            if (craftingItem.itemId().equals(itemId)) {
                return craftingItem;
            }
        }
        return null;
    }

    public static boolean isRegistered(CraftingMaterialFamily craftingItem) {
        return craftingItem != null && all().contains(craftingItem);
    }

    public static void register(CraftingMaterialFamily craftingItem) {
        if (craftingItem == null || craftingItem.itemId() == null || craftingItem.key() == null) {
            throw new IllegalArgumentException("a crafting item needs an item id and a key");
        }
        if (CraftingInhibitor.byItemId(craftingItem.itemId()) != null || ItemDisassembly.FEEDSTOCK_ITEM_ID.equals(craftingItem.itemId())) {
            throw new IllegalArgumentException(craftingItem.itemId() + " is already a crafting material");
        }
        List<CraftingMaterialFamily> previous;
        List<CraftingMaterialFamily> updated;
        do {
            previous = REGISTERED.get();
            for (CraftingMaterialFamily known : previous) {
                if (known.itemId().equals(craftingItem.itemId()) || known.key().equals(craftingItem.key())) {
                    throw new IllegalArgumentException("a crafting item with the item id " + craftingItem.itemId() + " or the key "
                            + craftingItem.key() + " is already registered");
                }
            }
            updated = new ArrayList<>(previous);
            updated.add(craftingItem);
            updated = List.copyOf(updated);
        } while (!REGISTERED.compareAndSet(previous, updated));
    }

    public static boolean unregister(CraftingMaterialFamily craftingItem) {
        if (craftingItem == null || BUILT_IN.contains(craftingItem)) {
            return false;
        }
        List<CraftingMaterialFamily> previous;
        List<CraftingMaterialFamily> updated;
        do {
            previous = REGISTERED.get();
            if (!previous.contains(craftingItem)) {
                return false;
            }
            updated = previous.stream().filter(known -> known != craftingItem).toList();
        } while (!REGISTERED.compareAndSet(previous, updated));
        for (ItemType type : ItemType.values()) {
            retract(craftingItem, type);
        }
        return true;
    }
}
