package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SpecialItemData;

import java.util.List;

public record SocketableItemData(String definitionId, long seed, List<RolledEffect> effects, FrozenName name) {

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
        FrozenName frozen = name;
        if (effects != null && frozen == null) {
            frozen = SocketableNames.freeze(definition(), seed, effects);
        }
        return new SpecialItemData(ITEM_ID, SocketableCodec.cargo(this, frozen));
    }

    public SocketableDefinition definition() {
        return SocketableDefinitions.get(definitionId);
    }

    public Socketable preview() {
        return create(null);
    }

    Socketable create(String instanceId) {
        SocketableDefinition definition = definition();
        if (definition == null) {
            return null;
        }
        Socketable socketable = definition.kind().create(instanceId, definitionId, seed,
                effects != null ? effects : SocketableRoller.roll(definition, seed));
        socketable.freezeName(name);
        return socketable;
    }
}
