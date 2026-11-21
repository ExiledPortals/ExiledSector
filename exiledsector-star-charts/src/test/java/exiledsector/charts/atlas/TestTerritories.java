package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.OfficerSeats;

public final class TestTerritories {

    public static final String RANK_ID = "lieutenant";
    public static final String TRAIT_ID = "steady";

    private TestTerritories() {
    }

    public static AtlasOfficer establish(AtlasState state, AtlasCell centre, String factionId) {
        AtlasTerritoryTransfer.dismissOfficer(state, centre);
        state.release(centre);
        int recordId = state.officers().stream().mapToInt(AtlasOfficer::recordId).max().orElse(0) + 1;
        AtlasOfficer officer = AtlasOfficer.waiting(recordId, factionId, RANK_ID, TRAIT_ID, new AtlasOfficer.OfficerSeeds(recordId, recordId + 1L,
                        recordId + 2L, recordId + 3L), null)
                .seatedAt(RANK_ID, lieutenantSeat(state))
                .takingCommand(centre, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, centre));
        state.putOfficer(officer);
        state.claim(centre, centre);
        return officer;
    }

    private static int lieutenantSeat(AtlasState state) {
        int places = OfficerRanks.all().get(0).places();
        for (int seat = 0; seat < places; seat++) {
            AtlasOfficer seated = OfficerSeats.seated(state.officers(), RANK_ID, seat);
            if (seated == null) {
                return seat;
            }
            if (!seated.isActive()) {
                state.removeOfficer(seated.recordId());
                return seat;
            }
        }
        return places;
    }
}
