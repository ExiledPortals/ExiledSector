package exiledsector.socketables;

import exiledsector.i18n.Translation;

import java.util.List;

public enum SocketableKind {

    SUBROUTINE("subroutine", Subroutine::new),
    OFFICER("officer", Officer::new),
    TEAM("team", Team::new),
    AI_CORE("ai_core", AiCore::new);

    private final String id;
    private final Factory factory;

    SocketableKind(String id, Factory factory) {
        this.id = id;
        this.factory = factory;
    }

    public static SocketableKind byId(String id) {
        for (SocketableKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown socketable kind: " + id);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return Translation.text("socketable.kind." + id);
    }

    Socketable create(String instanceId, String definitionId, long seed, List<RolledEffect> effects) {
        return factory.create(instanceId, definitionId, seed, effects);
    }

    @FunctionalInterface
    private interface Factory {
        Socketable create(String instanceId, String definitionId, long seed, List<RolledEffect> effects);
    }
}
