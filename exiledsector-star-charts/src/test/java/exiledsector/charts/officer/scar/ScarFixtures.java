package exiledsector.charts.officer.scar;

import exiledsector.charts.ChartTables;

import java.util.List;

final class ScarFixtures {

    static final HullFacts SHIELDED_WARSHIP = new HullFacts(true, true, false, false, true, false, false, false, false, false);
    static final HullFacts OMNI_CARRIER = new HullFacts(true, false, true, true, false, false, false, false, false, true);
    static final HullFacts UNSHIELDED_WARSHIP = new HullFacts(false, false, false, false, true, false, false, false, false, false);
    static final HullFacts PHASE_SHIP = new HullFacts(false, false, false, false, true, true, false, false, false, false);
    static final HullFacts AUTOMATED_TIMELESS = new HullFacts(true, false, true, false, true, false, false, true, true, false);

    private ScarFixtures() {
    }

    static List<String> loadShipped() throws Exception {
        return ScarTable.register(ChartTables.csvRows(ScarTable.PATH));
    }

    static ScarRow row(String id, ScarDetector detector, float threshold, String themeTag, ScarRow.Effect... effects) {
        return new ScarRow(id, detector, threshold, themeTag, List.of(effects), List.of(), true, 0);
    }

    static ScarRow fallback() {
        return new ScarRow(ScarTable.FALLBACK_SCAR_ID, null, 0f, "", List.of(), List.of(), true, 2);
    }
}
