package exiledsector.ui.socket;

import exiledsector.items.Item;
import exiledsector.items.ItemTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

final class ItemStorageStacks {

    private static final class Stack {
        private Item shownItem;
        private boolean shownItemPreferred;
        private int newestStoreOrder;
        private int size;

        void add(Item item, int storeOrder, boolean preferred) {
            size++;
            newestStoreOrder = storeOrder;
            if (!shownItemPreferred) {
                shownItem = item;
                shownItemPreferred = preferred;
            }
        }
    }

    private ItemStorageStacks() {
    }

    static List<ItemStorageRow> rows(List<Item> ownedItems, Predicate<Item> shown, Function<Item, String> installedShipLookup,
                                     Predicate<Item> preferredInStack) {
        List<ItemStorageRow> rows = new ArrayList<>(ownedItems.size());
        Map<String, Stack> stacksByDefinitionId = new LinkedHashMap<>();
        for (int storeOrder = 0; storeOrder < ownedItems.size(); storeOrder++) {
            Item item = ownedItems.get(storeOrder);
            if (!shown.test(item)) {
                continue;
            }
            String installedShipName = installedShipLookup.apply(item);
            if (installedShipName != null || !ItemTypes.stacksInStorage(item)) {
                rows.add(ItemStorageRow.of(item, storeOrder, installedShipName, 1));
            } else {
                stacksByDefinitionId.computeIfAbsent(item.definitionId(), definitionId -> new Stack()).add(item, storeOrder, preferredInStack.test(item));
            }
        }
        stacksByDefinitionId.values().forEach(stack -> rows.add(ItemStorageRow.of(stack.shownItem, stack.newestStoreOrder, null, stack.size)));
        return rows;
    }
}
