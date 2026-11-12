package exiledsector.charts.officer.turn;

public record TurnCause(Kind kind, int officerRecordId) {

    public enum Kind {
        OFFICER_DEFEATED,
        OFFICER_VICTORIOUS,
        BOSS_KILLS,
        FORCED
    }

    public static TurnCause officerDefeated(int recordId) {
        return new TurnCause(Kind.OFFICER_DEFEATED, recordId);
    }

    public static TurnCause officerVictorious(int recordId) {
        return new TurnCause(Kind.OFFICER_VICTORIOUS, recordId);
    }

    public static TurnCause bossKills() {
        return new TurnCause(Kind.BOSS_KILLS, Intent.NO_OFFICER);
    }

    public static TurnCause forced() {
        return new TurnCause(Kind.FORCED, Intent.NO_OFFICER);
    }
}
