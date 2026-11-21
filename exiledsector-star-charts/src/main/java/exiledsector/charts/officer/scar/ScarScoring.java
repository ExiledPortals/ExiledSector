package exiledsector.charts.officer.scar;

import java.util.List;
import java.util.Map;

public final class ScarScoring {

    static final float WINNING_SCORE = 1f;

    private ScarScoring() {
    }

    public static boolean eligible(ScarRow row, Map<String, Integer> currentScars, HullFacts hull) {
        if (row == null) {
            return false;
        }
        if (ScarTable.FALLBACK_SCAR_ID.equals(row.id())) {
            return true;
        }
        int stacks = currentScars == null ? 0 : currentScars.getOrDefault(row.id(), 0);
        return stacks < ScarTable.STACK_CAP && hull != null && row.fits(hull);
    }

    public static String pick(ScarEvidence evidence, List<ScarRow> rows, Map<String, Integer> currentScars, HullFacts hull) {
        String best = ScarTable.FALLBACK_SCAR_ID;
        float bestScore = WINNING_SCORE;
        boolean found = false;
        for (ScarRow row : rows) {
            if (row.detector() == null || !eligible(row, currentScars, hull)) {
                continue;
            }
            float score = row.score(evidence);
            if (Float.isFinite(score) && (found ? score > bestScore : score >= bestScore)) {
                best = row.id();
                bestScore = score;
                found = true;
            }
        }
        return best;
    }
}
