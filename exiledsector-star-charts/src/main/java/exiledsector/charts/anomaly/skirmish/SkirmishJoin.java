package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.turn.Intent;

public record SkirmishJoin(SkirmishSide side, String attackerId, Intent contest) {

    private static final int FIELD_COUNT = 6;
    private static final int CONTEST_FIELD = 2;
    private static final String NO_CONTEST = ",,,";

    public SkirmishJoin {
        side = side == null ? SkirmishSide.NEITHER : side;
        attackerId = attackerId == null || attackerId.isEmpty() ? null : attackerId;
    }

    public String encode() {
        return StateFields.join(side.key(), attackerId == null ? "" : attackerId, contest == null ? NO_CONTEST : contest.encode());
    }

    static SkirmishJoin decode(String data) {
        return StateFields.decode(data, FIELD_COUNT, fields -> new SkirmishJoin(SkirmishSide.parse(fields.text(0)), fields.text(1),
                Intent.decode(StateFields.join(fields.text(CONTEST_FIELD), fields.text(CONTEST_FIELD + 1), fields.text(CONTEST_FIELD + 2),
                        fields.text(CONTEST_FIELD + 3)))));
    }

    public static SkirmishJoin of(AtlasRunNode node) {
        return node == null ? null : decode(node.anomalyData(FactionSkirmish.ANOMALY_ID));
    }

    static SkirmishJoin of(ChartGenContext context) {
        return context == null ? null : decode(context.anomalyData(FactionSkirmish.ANOMALY_ID));
    }
}
