package exiledsector.items;

import com.fs.starfarer.api.campaign.SpecialItemData;

import java.util.List;

public record ItemData(String definitionId, long seed, List<RolledModifier> explicits, FrozenName frozenName, ItemTraits traits) {

    public static final String ITEM_ID = "exiledSector_socketable";
    public static final String STAR_CHART_ITEM_ID = "exiledSector_star_chart";

    public ItemData {
        definitionId = ItemIdAliases.currentId(definitionId);
        explicits = explicits == null ? null : List.copyOf(explicits);
    }

    public ItemData(String definitionId, long seed, List<RolledModifier> explicits, FrozenName frozenName) {
        this(definitionId, seed, explicits, frozenName, null);
    }

    public ItemData(String definitionId, long seed) {
        this(definitionId, seed, null, null);
    }

    public static ItemData rolled(ItemDefinition definition, long seed) {
        if (definition.type() == ItemType.STAR_CHART) {
            return rolledAtTier(definition, seed, StarChart.LOWEST_TIER);
        }
        List<RolledModifier> modifiers = ItemRoller.roll(definition, seed);
        return new ItemData(definition.id(), seed, modifiers, ItemNames.freeze(definition, seed, modifiers));
    }

    public static ItemData rolledAtTier(ItemDefinition definition, long seed, int tier) {
        List<RolledModifier> modifiers = ItemRoller.rollAtTier(definition, seed, tier);
        ItemTraits tierTraits = definition.type() == ItemType.STAR_CHART ? ItemTraits.ofTier(StarChart.clampTier(tier)) : null;
        return new ItemData(definition.id(), seed, modifiers, ItemNames.freeze(definition, seed, modifiers), tierTraits);
    }

    public static ItemData of(Item item) {
        return new ItemData(item.definitionId(), item.seed(), item.explicits(), item.frozenName(), item.traits());
    }

    public static ItemData parse(String data) {
        return ItemCodec.decode(data);
    }

    public static ItemData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public static ItemData ofStarChart(SpecialItemData special) {
        ItemData chart = special != null && STAR_CHART_ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
        return chart == null || chart.traits() != null ? chart : chart.withTraits(ItemTraits.ofTier(StarChart.LOWEST_TIER));
    }

    public boolean isStarChart() {
        ItemDefinition itemDefinition = definition();
        return itemDefinition != null ? itemDefinition.type() == ItemType.STAR_CHART : traits != null;
    }

    public ItemData withTraits(ItemTraits replacementTraits) {
        return new ItemData(definitionId, seed, explicits, frozenName, replacementTraits);
    }

    public SpecialItemData toSpecialItem() {
        FrozenName resolvedName = frozenName;
        if (explicits != null && resolvedName == null) {
            resolvedName = ItemNames.freeze(definition(), seed, explicits);
        }
        return new SpecialItemData(isStarChart() ? STAR_CHART_ITEM_ID : ITEM_ID, ItemCodec.cargo(this, resolvedName));
    }

    public String encode() {
        return ItemCodec.cargo(this, frozenName);
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
        List<RolledModifier> modifiers = explicits;
        if (modifiers == null) {
            modifiers = traits == null ? ItemRoller.roll(itemDefinition, seed) : ItemRoller.rollAtTier(itemDefinition, seed, traits.tier());
        }
        Item item = Item.create(instanceId, definitionId, seed, modifiers, traits);
        item.freezeName(frozenName);
        return item;
    }
}
