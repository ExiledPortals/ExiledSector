package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;

@FunctionalInterface
public interface OfficerTurnListener {

    void turnTaken(AtlasGenerator generator, AtlasState state, TurnResult result, AtlasCell openRunCell);
}
