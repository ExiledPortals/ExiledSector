package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.turn.Intent;

public record SkirmishFight(String attackerFactionId, String attackerRankId, String defenderRankId, Intent contest) {

    private static final int FIELD_COUNT = 7;
    private static final int CONTEST_FIELD = 3;
    private static final int OLD_JOIN_FIELD_COUNT = 6;
    private static final int OLD_JOIN_ATTACKER_FIELD = 1;
    private static final int OLD_JOIN_CONTEST_FIELD = 2;
    private static final String NO_CONTEST = ",,,";

    public SkirmishFight {
        attackerFactionId = emptyAsNull(attackerFactionId);
        attackerRankId = emptyAsNull(attackerRankId);
        defenderRankId = emptyAsNull(defenderRankId);
    }

    public boolean betweenOfficers() {
        return contest != null && contest.officerRecordId() != Intent.NO_OFFICER && contest.targetRecordId() != Intent.NO_OFFICER
                && attackerFactionId != null && attackerRankId != null && defenderRankId != null;
    }

    public int attackerRecordId() {
        return contest == null ? Intent.NO_OFFICER : contest.officerRecordId();
    }

    public int defenderRecordId() {
        return contest == null ? Intent.NO_OFFICER : contest.targetRecordId();
    }

    public String encode() {
        return StateFields.join(textOf(attackerFactionId), textOf(attackerRankId), textOf(defenderRankId), contest == null ? NO_CONTEST : contest.encode());
    }

    static SkirmishFight decode(String data) {
        SkirmishFight fight = StateFields.decode(data, FIELD_COUNT, fields -> new SkirmishFight(fields.text(0), fields.text(1), fields.text(2),
                contestAt(fields, CONTEST_FIELD)));
        return fight != null ? fight : StateFields.decode(data, OLD_JOIN_FIELD_COUNT, fields -> new SkirmishFight(fields.text(OLD_JOIN_ATTACKER_FIELD),
                null, null, contestAt(fields, OLD_JOIN_CONTEST_FIELD)));
    }

    public static SkirmishFight of(AtlasRunNode node) {
        return node == null ? null : decode(node.anomalyData(FactionSkirmish.ANOMALY_ID));
    }

    static SkirmishFight of(ChartGenContext context) {
        return context == null ? null : decode(context.anomalyData(FactionSkirmish.ANOMALY_ID));
    }

    private static Intent contestAt(StateFields fields, int firstField) {
        return Intent.decode(StateFields.join(fields.text(firstField), fields.text(firstField + 1), fields.text(firstField + 2), fields.text(firstField + 3)));
    }

    private static String emptyAsNull(String text) {
        return text == null || text.isEmpty() ? null : text;
    }

    private static String textOf(String nullable) {
        return nullable == null ? "" : nullable;
    }
}
