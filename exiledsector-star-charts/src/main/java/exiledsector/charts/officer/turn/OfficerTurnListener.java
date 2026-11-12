package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasState;

@FunctionalInterface
public interface OfficerTurnListener {

    void turnTaken(AtlasState state, TurnResult result);
}
