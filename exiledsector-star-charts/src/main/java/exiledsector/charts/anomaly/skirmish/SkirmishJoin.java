package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.generation.ChartGenContext;

public record SkirmishJoin(SkirmishSide side, String attackerId) {

    public SkirmishJoin {
        side = side == null ? SkirmishSide.NEITHER : side;
        attackerId = attackerId == null || attackerId.isEmpty() ? null : attackerId;
    }

    public String encode() {
        return StateFields.join(side.key(), attackerId == null ? "" : attackerId);
    }

    static SkirmishJoin decode(String data) {
        return StateFields.decode(data, 2, fields -> new SkirmishJoin(SkirmishSide.parse(fields.text(0)), fields.text(1)));
    }

    public static SkirmishJoin of(AtlasRunNode node) {
        return node == null ? null : decode(node.anomalyData(FactionSkirmish.ANOMALY_ID));
    }

    static SkirmishJoin of(ChartGenContext context) {
        return context == null ? null : decode(context.anomalyData(FactionSkirmish.ANOMALY_ID));
    }
}
