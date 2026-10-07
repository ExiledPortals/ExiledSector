package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SpecialItemData;

import java.util.List;

public record SocketableItemData(String definitionId, long seed, List<RolledEffect> effects, FrozenName frozenName) {

    public static final String ITEM_ID = "exiledSector_socketable";

    public SocketableItemData {
        effects = effects == null ? null : List.copyOf(effects);
    }

    public SocketableItemData(String definitionId, long seed) {
        this(definitionId, seed, null, null);
    }

    public static SocketableItemData rolled(SocketableDefinition definition, long seed) {
        List<RolledEffect> effects = SocketableRoller.roll(definition, seed);
        return new SocketableItemData(definition.id(), seed, effects, SocketableNames.freeze(definition, seed, effects));
    }

    public static SocketableItemData parse(String data) {
        return SocketableCodec.decode(data);
    }

    public static SocketableItemData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public SpecialItemData toSpecialItem() {
        FrozenName resolvedName = frozenName;
        if (effects != null && resolvedName == null) {
            resolvedName = SocketableNames.freeze(definition(), seed, effects);
        }
        return new SpecialItemData(ITEM_ID, SocketableCodec.cargo(this, resolvedName));
    }

    public SocketableDefinition definition() {
        return SocketableDefinitions.get(definitionId);
    }

    public Socketable preview() {
        return create(null);
    }

    Socketable create(String instanceId) {
        SocketableDefinition itemDefinition = definition();
        if (itemDefinition == null) {
            return null;
        }
        Socketable socketable = itemDefinition.kind().create(instanceId, definitionId, seed,
                effects != null ? effects : SocketableRoller.roll(itemDefinition, seed));
        socketable.freezeName(frozenName);
        return socketable;
    }
}
