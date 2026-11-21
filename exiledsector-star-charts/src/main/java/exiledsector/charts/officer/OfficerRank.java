package exiledsector.charts.officer;

import exiledsector.i18n.Translation;

public record OfficerRank(String id, int bossSizePercent, int fleetStrengthPercent, int actions, int places) {

    public String displayName() {
        return Translation.gameText("chart.officer.rank." + id);
    }
}
