package exiledsector.items;

import java.util.Collection;

public final class Consumables {

    private Consumables() {
    }

    public static <T, R> R use(Consumable<T, R> consumable, T target, CraftingMaterials materials) {
        if (materials.count(consumable.itemId()) < 1 || consumable.requiredItemIds().stream().anyMatch(itemId -> materials.count(itemId) < 1)
                || !consumable.check(target).allowed()) {
            return null;
        }
        Collection<String> alongside = consumable.spentAlongside(target);
        R result = consumable.apply(target);
        if (result != null) {
            materials.take(consumable.itemId(), 1);
            alongside.forEach(itemId -> materials.take(itemId, 1));
        }
        return result;
    }
}
