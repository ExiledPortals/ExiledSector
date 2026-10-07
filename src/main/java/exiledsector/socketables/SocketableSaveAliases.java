package exiledsector.socketables;

import com.thoughtworks.xstream.XStream;

import java.util.List;
import java.util.Map;

public final class SocketableSaveAliases {

    static final List<String> LEGACY_SOCKETABLE_ALIASES = List.of(
            "exiledSector.Subroutine", "exiledSector.Officer", "exiledSector.Team", "exiledSector.AiCore");
    static final Map<String, Class<?>> ALIASES = Map.of(
            "exiledSector.SocketableStore", SocketableStore.class,
            "exiledSector.RolledEffect", RolledEffect.class,
            "exiledSector.FrozenName", FrozenName.class,
            "exiledSector.HullFramework", HullFramework.class,
            "exiledSector.Socketable", Socketable.class);

    private SocketableSaveAliases() {
    }

    public static void register(XStream xstream) {
        LEGACY_SOCKETABLE_ALIASES.forEach(legacyAlias -> xstream.alias(legacyAlias, Socketable.class));
        ALIASES.forEach(xstream::alias);
    }
}
