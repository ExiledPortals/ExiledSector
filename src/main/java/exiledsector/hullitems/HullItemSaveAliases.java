package exiledsector.hullitems;

import exiledsector.modifiers.ModifierSlot;
import com.thoughtworks.xstream.XStream;

import java.util.List;
import java.util.Map;

public final class HullItemSaveAliases {

    static final List<String> LEGACY_HULL_ITEM_ALIASES = List.of(
            "exiledSector.Subroutine", "exiledSector.Officer", "exiledSector.Team", "exiledSector.AiCore");
    static final Map<String, Class<?>> ALIASES = Map.of(
            "exiledSector.SocketableStore", HullItemStore.class,
            "exiledSector.RolledEffect", RolledEffect.class,
            "exiledSector.FrozenName", FrozenName.class,
            "exiledSector.Socketable", HullItem.class,
            "exiledSector.RolledModifier", RolledModifier.class,
            "exiledSector.ModifierSlot", ModifierSlot.class);

    private HullItemSaveAliases() {
    }

    public static void register(XStream xstream) {
        LEGACY_HULL_ITEM_ALIASES.forEach(legacyAlias -> xstream.alias(legacyAlias, HullItem.class));
        ALIASES.forEach(xstream::alias);
    }
}
