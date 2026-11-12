package exiledsector.charts.officer.turn;

import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerDefeat;
import exiledsector.charts.officer.turn.TurnResult.LossKind;
import exiledsector.charts.officer.turn.TurnResult.OfficerLoss;

public final class ChallengeLossScars implements OfficerTurnListener {

    public static final ChallengeLossScars INSTANCE = new ChallengeLossScars();
    static final String SEED_PREFIX = "officerChallengeScar|";

    private ChallengeLossScars() {
    }

    public static void register() {
        OfficerTurns.removeListener(INSTANCE);
        OfficerTurns.addListener(INSTANCE);
    }

    @Override
    public void turnTaken(AtlasState state, TurnResult result) {
        for (OfficerLoss loss : result.losses()) {
            if (loss.kind() != LossKind.LOST_CHALLENGE || loss.died()) {
                continue;
            }
            AtlasOfficer winner = state.officer(loss.winnerRecordId());
            String winnerFactionId = winner == null ? null : winner.factionId();
            OfficerDefeat.challengeLost(state, loss.recordId(), winnerFactionId,
                    Seeds.random(SEED_PREFIX + result.turn() + "|" + loss.recordId()));
        }
    }
}
