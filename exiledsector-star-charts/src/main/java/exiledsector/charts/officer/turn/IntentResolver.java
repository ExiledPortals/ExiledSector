package exiledsector.charts.officer.turn;

@FunctionalInterface
public interface IntentResolver {

    IntentResolver ABSTRACT = Contest::abstractAttackerWins;

    boolean attackerWins(Contest contest);
}
