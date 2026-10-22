package exiledsector.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ItemTypes {

    private static final Map<ItemType, ItemTypeSupport> SUPPORT_BY_TYPE = Collections.synchronizedMap(new EnumMap<>(ItemType.class));

    private ItemTypes() {
    }

    public static void register(ItemTypeSupport support) {
        SUPPORT_BY_TYPE.put(support.type(), support);
    }

    static ItemTypeSupport of(ItemType type) {
        return type == null ? null : SUPPORT_BY_TYPE.get(type);
    }

    static List<ItemTypeSupport> all() {
        synchronized (SUPPORT_BY_TYPE) {
            return List.copyOf(SUPPORT_BY_TYPE.values());
        }
    }

    static ItemTypeSupport ownerOfItemId(String itemId) {
        for (ItemTypeSupport support : all()) {
            if (support.ownsItemId(itemId)) {
                return support;
            }
        }
        return null;
    }

    static int lowestTier(ItemType type, String definitionId) {
        ItemTypeSupport support = of(type);
        return support == null ? ItemTraits.LOWEST_TIER : support.lowestTier(definitionId);
    }

    static int rolledTier(ItemDefinition definition, int tier) {
        ItemTypeSupport support = of(definition.type());
        return support == null ? tier : support.rolledTier(definition.id(), tier);
    }

    static boolean rollsAtTier(ItemDefinition definition, int tier) {
        ItemTypeSupport support = of(definition.type());
        return support == null || support.rollsAtTier(definition.id(), tier);
    }

    static boolean inGenericPools(ItemDefinition definition) {
        ItemTypeSupport support = of(definition.type());
        return support == null || support.inGenericPools(definition);
    }

    static List<ItemData> rollSalvage(Random random, float chance) {
        List<ItemData> rolled = new ArrayList<>();
        for (ItemTypeSupport support : all()) {
            rolled.addAll(support.rollSalvage(random, chance));
        }
        return rolled;
    }

    static boolean uniqueRarity(ItemDefinition definition) {
        ItemTypeSupport support = of(definition.type());
        return support != null && support.uniqueRarity(definition);
    }

    static boolean lockedFromCurrency(ItemDefinition definition) {
        ItemTypeSupport support = of(definition.type());
        return support != null && support.lockedFromCurrency(definition);
    }
}
