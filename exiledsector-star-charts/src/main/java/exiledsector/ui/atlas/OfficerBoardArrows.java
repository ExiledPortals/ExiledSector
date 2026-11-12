package exiledsector.ui.atlas;

import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import exiledsector.charts.officer.turn.TurnResult;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

final class OfficerBoardArrows {

    static final float HOVERED_ALPHA = 1f;
    static final float FADED_ALPHA = 0.15f;
    static final float RESTING_ALPHA = 0.6f;

    enum Style { SOLID, DASHED }

    record Arrow(int fromRecordId, int toRecordId, Style style) {

        boolean touches(int recordId) {
            return fromRecordId == recordId || toRecordId == recordId;
        }
    }

    private OfficerBoardArrows() {
    }

    static List<Arrow> arrows(List<TurnResult.ResolvedIntent> lastTurn, List<Intent> declared, IntPredicate shown) {
        Set<Arrow> arrows = new LinkedHashSet<>();
        for (TurnResult.ResolvedIntent resolved : lastTurn) {
            if (resolved.kind().isConflict() && resolved.defenderRecordId() != Intent.NO_OFFICER) {
                int winner = resolved.succeeded() ? resolved.attackerRecordId() : resolved.defenderRecordId();
                int loser = resolved.succeeded() ? resolved.defenderRecordId() : resolved.attackerRecordId();
                addIfShown(arrows, new Arrow(winner, loser, Style.SOLID), shown);
            }
        }
        for (Intent intent : declared) {
            if (intent.kind().isConflict() && intent.targetRecordId() != Intent.NO_OFFICER) {
                addIfShown(arrows, new Arrow(intent.officerRecordId(), intent.targetRecordId(), Style.DASHED), shown);
            }
        }
        return List.copyOf(arrows);
    }

    private static void addIfShown(Set<Arrow> arrows, Arrow arrow, IntPredicate shown) {
        if (arrow.fromRecordId() != arrow.toRecordId() && shown.test(arrow.fromRecordId()) && shown.test(arrow.toRecordId())) {
            arrows.add(arrow);
        }
    }

    static Set<Integer> shields(List<Intent> declared, IntPredicate shown) {
        Set<Integer> shielded = new LinkedHashSet<>();
        for (Intent intent : declared) {
            if (intent.kind() == IntentKind.DEFEND && shown.test(intent.officerRecordId())) {
                shielded.add(intent.officerRecordId());
            }
        }
        return shielded;
    }

    static float alpha(Arrow arrow, Integer hoveredRecordId) {
        if (hoveredRecordId == null) {
            return RESTING_ALPHA;
        }
        return arrow.touches(hoveredRecordId) ? HOVERED_ALPHA : FADED_ALPHA;
    }

    static float[] curve(float fromX, float fromY, float toX, float toY, float trim, float bulgeRatio, int segments) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= trim * 2f) {
            return new float[0];
        }
        float unitX = dx / length;
        float unitY = dy / length;
        float startX = fromX + unitX * trim;
        float startY = fromY + unitY * trim;
        float endX = toX - unitX * trim;
        float endY = toY - unitY * trim;
        float bulge = length * bulgeRatio;
        float controlX = (startX + endX) / 2f + unitY * bulge;
        float controlY = (startY + endY) / 2f - unitX * bulge;
        float[] points = new float[(segments + 1) * 2];
        for (int step = 0; step <= segments; step++) {
            float t = step / (float) segments;
            float u = 1f - t;
            points[step * 2] = u * u * startX + 2f * u * t * controlX + t * t * endX;
            points[step * 2 + 1] = u * u * startY + 2f * u * t * controlY + t * t * endY;
        }
        return points;
    }
}
