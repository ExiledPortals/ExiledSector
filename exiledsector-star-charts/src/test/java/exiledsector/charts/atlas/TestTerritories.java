package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;

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
                .takingCommand(centre, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, centre));
        state.putOfficer(officer);
        state.claim(centre, centre);
        return officer;
    }
}
