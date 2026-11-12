package exiledsector.charts.officer.scar;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScarBattleResults {

    static final int MAX_KEPT = 8;
    private static final Map<String, ScarEvidence> EVIDENCE_BY_FLAGSHIP_ID = new LinkedHashMap<>();

    private ScarBattleResults() {
    }

    static synchronized void publish(String flagshipMemberId, ScarEvidence evidence) {
        if (flagshipMemberId == null || evidence == null) {
            return;
        }
        EVIDENCE_BY_FLAGSHIP_ID.remove(flagshipMemberId);
        EVIDENCE_BY_FLAGSHIP_ID.put(flagshipMemberId, evidence);
        while (EVIDENCE_BY_FLAGSHIP_ID.size() > MAX_KEPT) {
            EVIDENCE_BY_FLAGSHIP_ID.remove(EVIDENCE_BY_FLAGSHIP_ID.keySet().iterator().next());
        }
    }

    public static synchronized ScarEvidence of(String flagshipMemberId) {
        return flagshipMemberId == null ? null : EVIDENCE_BY_FLAGSHIP_ID.get(flagshipMemberId);
    }

    public static synchronized ScarEvidence take(String flagshipMemberId) {
        return flagshipMemberId == null ? null : EVIDENCE_BY_FLAGSHIP_ID.remove(flagshipMemberId);
    }

    public static synchronized void clear() {
        EVIDENCE_BY_FLAGSHIP_ID.clear();
    }
}
