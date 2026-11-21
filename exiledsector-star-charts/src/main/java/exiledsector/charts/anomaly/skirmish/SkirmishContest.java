package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.turn.Intent;

public record SkirmishContest(Intent intent, AtlasOfficer attacker, AtlasOfficer defender) {
}
