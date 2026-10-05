package exiledsector.socketables;

import java.util.List;

public final class Officer extends Socketable {

    Officer(String id, String definitionId, long seed, List<RolledEffect> effects) {
        super(id, definitionId, seed, effects);
    }

    @Override
    public SocketableKind kind() {
        return SocketableKind.OFFICER;
    }
}
