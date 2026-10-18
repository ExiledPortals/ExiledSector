package exiledsector.items;

import com.fs.starfarer.api.campaign.SpecialItemData;

import java.util.List;

public record ItemData(String definitionId, long seed, List<RolledModifier> explicits, FrozenName frozenName) {

    public static final String ITEM_ID = "exiledSector_socketable";

    public ItemData {
        definitionId = ItemIdAliases.currentId(definitionId);
        explicits = explicits == null ? null : List.copyOf(explicits);
    }

    public ItemData(String definitionId, long seed) {
        this(definitionId, seed, null, null);
    }

    public static ItemData rolled(ItemDefinition definition, long seed) {
        List<RolledModifier> modifiers = ItemRoller.roll(definition, seed);
        return new ItemData(definition.id(), seed, modifiers, ItemNames.freeze(definition, seed, modifiers));
    }

    public static ItemData parse(String data) {
        return ItemCodec.decode(data);
    }

    public static ItemData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public SpecialItemData toSpecialItem() {
        FrozenName resolvedName = frozenName;
        if (explicits != null && resolvedName == null) {
            resolvedName = ItemNames.freeze(definition(), seed, explicits);
        }
        return new SpecialItemData(ITEM_ID, ItemCodec.cargo(this, resolvedName));
    }

    public ItemDefinition definition() {
        return ItemDefinitions.get(definitionId);
    }

    public Item preview() {
        return create(null);
    }

    Item create(String instanceId) {
        ItemDefinition itemDefinition = definition();
        if (itemDefinition == null) {
            return null;
        }
        Item item = Item.create(instanceId, definitionId, seed,
                explicits != null ? explicits : ItemRoller.roll(itemDefinition, seed));
        item.freezeName(frozenName);
        return item;
    }
}
