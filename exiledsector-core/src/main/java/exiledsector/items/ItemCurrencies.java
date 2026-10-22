package exiledsector.items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ItemCurrencies {

    private static final List<ItemCurrency> BUILT_IN = List.of(CraftingCurrency.values());
    private static final AtomicReference<List<ItemCurrency>> REGISTERED = new AtomicReference<>(BUILT_IN);
    private static final AtomicReference<Set<Extension>> EXTENSIONS = new AtomicReference<>(Set.of());

    private record Extension(ItemCurrency currency, ItemType type) {
    }

    private ItemCurrencies() {
    }

    public static List<ItemCurrency> all() {
        return REGISTERED.get();
    }

    public static List<ItemCurrency> forType(ItemType type) {
        return all().stream().filter(currency -> appliesTo(currency, type)).toList();
    }

    public static boolean appliesTo(ItemCurrency currency, ItemType type) {
        if (currency == null || type == null) {
            return false;
        }
        return currency.appliesTo(type) || EXTENSIONS.get().contains(new Extension(currency, type));
    }

    public static boolean extend(ItemCurrency currency, ItemType type) {
        if (currency == null || type == null || !all().contains(currency)) {
            throw new IllegalArgumentException("only a registered currency can be extended to another item type");
        }
        if (currency.appliesTo(type)) {
            return false;
        }
        Set<Extension> previous;
        Set<Extension> updated;
        do {
            previous = EXTENSIONS.get();
            if (previous.contains(new Extension(currency, type))) {
                return false;
            }
            Set<Extension> extended = new HashSet<>(previous);
            extended.add(new Extension(currency, type));
            updated = Set.copyOf(extended);
        } while (!EXTENSIONS.compareAndSet(previous, updated));
        return true;
    }

    public static boolean retract(ItemCurrency currency, ItemType type) {
        Set<Extension> previous;
        Set<Extension> updated;
        do {
            previous = EXTENSIONS.get();
            if (!previous.contains(new Extension(currency, type))) {
                return false;
            }
            Set<Extension> remaining = new HashSet<>(previous);
            remaining.remove(new Extension(currency, type));
            updated = Set.copyOf(remaining);
        } while (!EXTENSIONS.compareAndSet(previous, updated));
        return true;
    }

    public static ItemCurrency byItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (ItemCurrency currency : all()) {
            if (currency.itemId().equals(itemId)) {
                return currency;
            }
        }
        return null;
    }

    public static boolean isRegistered(ItemCurrency currency) {
        return currency != null && all().contains(currency);
    }

    public static void register(ItemCurrency currency) {
        if (currency == null || currency.itemId() == null || currency.key() == null) {
            throw new IllegalArgumentException("a currency needs an item id and a key");
        }
        if (CraftingWard.byItemId(currency.itemId()) != null || ItemDisassembly.PARTS_ITEM_ID.equals(currency.itemId())) {
            throw new IllegalArgumentException(currency.itemId() + " is already a crafting material");
        }
        List<ItemCurrency> previous;
        List<ItemCurrency> updated;
        do {
            previous = REGISTERED.get();
            for (ItemCurrency known : previous) {
                if (known.itemId().equals(currency.itemId()) || known.key().equals(currency.key())) {
                    throw new IllegalArgumentException("a currency with the item id " + currency.itemId() + " or the key "
                            + currency.key() + " is already registered");
                }
            }
            updated = new ArrayList<>(previous);
            updated.add(currency);
            updated = List.copyOf(updated);
        } while (!REGISTERED.compareAndSet(previous, updated));
    }

    public static boolean unregister(ItemCurrency currency) {
        if (currency == null || BUILT_IN.contains(currency)) {
            return false;
        }
        List<ItemCurrency> previous;
        List<ItemCurrency> updated;
        do {
            previous = REGISTERED.get();
            if (!previous.contains(currency)) {
                return false;
            }
            updated = previous.stream().filter(known -> known != currency).toList();
        } while (!REGISTERED.compareAndSet(previous, updated));
        for (ItemType type : ItemType.values()) {
            retract(currency, type);
        }
        return true;
    }
}
