package exiledsector.socketables;

import java.util.List;

public final class Team extends Socketable {

    Team(String id, String definitionId, long seed, List<RolledEffect> effects) {
        super(id, definitionId, seed, effects);
    }

    @Override
    public SocketableKind kind() {
        return SocketableKind.TEAM;
    }
}
