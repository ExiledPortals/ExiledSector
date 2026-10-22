package exiledlib.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;

public interface ItemTypeSupport {

    ItemType type();

    Class<? extends Item> itemClass();

    String saveAlias();

    Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers);

    Item convert(Item source);

    default String definitionsPath() {
        return null;
    }

    default ItemDefinition parseDefinition(JSONObject row) {
        throw new IllegalArgumentException(type().id() + " definitions are not read from rows");
    }

    default boolean available(SectorAPI sector) {
        return true;
    }

    default int uniqueMinPlayerLevel() {
        return 0;
    }

    default boolean ownsItemId(String itemId) {
        return false;
    }

    default Item resolveItem(String itemId) {
        return null;
    }

    default Integer cargoPrice(Item item, boolean forSale) {
        return null;
    }

    default boolean inGenericPools(ItemDefinition definition) {
        return true;
    }

    default List<ItemData> rollSalvage(Random random, float chance) {
        return List.of();
    }

    default boolean uniqueRarity(ItemDefinition definition) {
        return false;
    }

    default boolean lockedFromCurrency(ItemDefinition definition) {
        return false;
    }

    default int lowestTier(String definitionId) {
        return ItemTraits.LOWEST_TIER;
    }

    default int rolledTier(String definitionId, int tier) {
        return tier;
    }

    default boolean rollsAtTier(String definitionId, int tier) {
        return true;
    }

    default ItemTraits traitsAtTier(ItemDefinition definition, long seed, int tier) {
        return null;
    }
}
