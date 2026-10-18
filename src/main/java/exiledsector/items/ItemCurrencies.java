package exiledsector.items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ItemCurrencies {

    private static final List<ItemCurrency> BUILT_IN = List.of(CraftingCurrency.values());
    private static final AtomicReference<List<ItemCurrency>> REGISTERED = new AtomicReference<>(BUILT_IN);

    private ItemCurrencies() {
    }

    public static List<ItemCurrency> all() {
        return REGISTERED.get();
    }

    public static List<ItemCurrency> forType(ItemType type) {
        return all().stream().filter(currency -> currency.appliesTo(type)).toList();
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
        return true;
    }
}
