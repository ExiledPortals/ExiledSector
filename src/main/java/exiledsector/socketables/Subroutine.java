package exiledsector.socketables;

import java.util.List;

public final class Subroutine extends Socketable {

    Subroutine(String id, String definitionId, long seed, List<RolledEffect> effects) {
        super(id, definitionId, seed, effects);
    }

    @Override
    public SocketableKind kind() {
        return SocketableKind.SUBROUTINE;
    }
}
