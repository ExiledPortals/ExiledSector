package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SpecialItemData;

public record SocketableItemData(String definitionId, long seed) {

    public static final String ITEM_ID = "exiledSector_socketable";
    private static final String SEPARATOR = "|";

    public static SocketableItemData parse(String data) {
        if (data == null) {
            return null;
        }
        int separator = data.lastIndexOf(SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        try {
            return new SocketableItemData(data.substring(0, separator), Long.parseLong(data.substring(separator + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static SocketableItemData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public SpecialItemData toSpecialItem() {
        return new SpecialItemData(ITEM_ID, definitionId + SEPARATOR + seed);
    }

    public SocketableDefinition definition() {
        return SocketableDefinitions.get(definitionId);
    }

    public Socketable preview() {
        SocketableDefinition definition = definition();
        if (definition == null) {
            return null;
        }
        return definition.kind().create(null, definitionId, seed, SocketableRoller.roll(definition, seed));
    }
}
