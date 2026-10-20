package exiledsector.ui.cargo;

public record DoctrineLook(DoctrineScreen screen, String factionId, String emblemPath) {

    public static DoctrineLook of(DoctrineScreen screen, String factionId) {
        return new DoctrineLook(screen, factionId, DoctrineIconFx.EMBLEM_PREFIX + factionId + ".png");
    }
}
