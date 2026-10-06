package exiledsector.socketables;

import com.thoughtworks.xstream.XStream;

import java.util.Map;

public final class SocketableSaveAliases {

    static final Map<String, Class<?>> ALIASES = Map.of(
            "exiledSector.SocketableStore", SocketableStore.class,
            "exiledSector.RolledEffect", RolledEffect.class,
            "exiledSector.FrozenName", FrozenName.class,
            "exiledSector.Subroutine", Subroutine.class,
            "exiledSector.Officer", Officer.class,
            "exiledSector.Team", Team.class,
            "exiledSector.AiCore", AiCore.class);

    private SocketableSaveAliases() {
    }

    public static void register(XStream xstream) {
        ALIASES.forEach(xstream::alias);
    }
}
