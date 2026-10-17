package exiledsector.hullitems;

import com.fs.starfarer.api.campaign.SpecialItemData;

import java.util.List;

public record HullItemData(String definitionId, long seed, List<RolledEffect> effects, FrozenName frozenName) {

    public static final String ITEM_ID = "exiledSector_socketable";

    public HullItemData {
        effects = effects == null ? null : List.copyOf(effects);
    }

    public HullItemData(String definitionId, long seed) {
        this(definitionId, seed, null, null);
    }

    public static HullItemData rolled(HullItemDefinition definition, long seed) {
        List<RolledEffect> effects = HullItemRoller.roll(definition, seed);
        return new HullItemData(definition.id(), seed, effects, HullItemNames.freeze(definition, seed, effects));
    }

    public static HullItemData parse(String data) {
        return HullItemCodec.decode(data);
    }

    public static HullItemData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public SpecialItemData toSpecialItem() {
        FrozenName resolvedName = frozenName;
        if (effects != null && resolvedName == null) {
            resolvedName = HullItemNames.freeze(definition(), seed, effects);
        }
        return new SpecialItemData(ITEM_ID, HullItemCodec.cargo(this, resolvedName));
    }

    public HullItemDefinition definition() {
        return HullItemDefinitions.get(definitionId);
    }

    public HullItem preview() {
        return create(null);
    }

    HullItem create(String instanceId) {
        HullItemDefinition itemDefinition = definition();
        if (itemDefinition == null) {
            return null;
        }
        HullItem hullItem = new HullItem(instanceId, definitionId, seed,
                effects != null ? effects : HullItemRoller.roll(itemDefinition, seed));
        hullItem.freezeName(frozenName);
        return hullItem;
    }
}
