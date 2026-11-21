package exiledsector.charts.officer.scar;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ScarChallengePick {

    private ScarChallengePick() {
    }

    public static String pick(Map<String, Integer> currentScars, HullFacts hull, Random random) {
        return pick(ScarTable.rows(), currentScars, hull, random);
    }

    static String pick(List<ScarRow> rows, Map<String, Integer> currentScars, HullFacts hull, Random random) {
        List<String> eligible = rows.stream()
                .filter(row -> !ScarTable.FALLBACK_SCAR_ID.equals(row.id()) && ScarScoring.eligible(row, currentScars, hull))
                .map(ScarRow::id)
                .toList();
        return eligible.isEmpty() ? ScarTable.FALLBACK_SCAR_ID : eligible.get(random.nextInt(eligible.size()));
    }
}
