package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import com.thoughtworks.xstream.XStream;

import java.util.List;
import java.util.Map;

public final class ItemSaveAliases {

    static final List<String> LEGACY_ITEM_ALIASES = List.of("exiledSector.Officer", "exiledSector.Team", "exiledSector.AiCore");
    static final String UNRESOLVED_ITEM_ALIAS = "exiledSector.Socketable";
    static final Map<String, Class<?>> ALIASES = Map.of(
            "exiledSector.SocketableStore", ItemStore.class,
            "exiledSector.RolledEffect", RolledLine.class,
            "exiledSector.FrozenName", FrozenName.class,
            "exiledSector.RolledModifier", RolledModifier.class,
            "exiledSector.ModifierSlot", ModifierSlot.class);
    static final List<FieldAlias> FIELD_ALIASES = List.of(new FieldAlias("locked", RolledModifier.class, "immutable"));

    record FieldAlias(String savedName, Class<?> definedIn, String fieldName) {
    }

    private ItemSaveAliases() {
    }

    public static void register(XStream xstream) {
        LEGACY_ITEM_ALIASES.forEach(legacyAlias -> xstream.alias(legacyAlias, UnresolvedItem.class));
        xstream.alias(UNRESOLVED_ITEM_ALIAS, UnresolvedItem.class);
        ALIASES.forEach(xstream::alias);
        FIELD_ALIASES.forEach(alias -> xstream.aliasField(alias.savedName(), alias.definedIn(), alias.fieldName()));
        ItemTypes.all().forEach(support -> xstream.alias(support.saveAlias(), support.itemClass()));
    }
}
