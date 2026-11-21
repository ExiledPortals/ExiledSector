package exiledsector.charts.atlas;

final class TerritorylessClaimRepair {

    private TerritorylessClaimRepair() {
    }

    static void repair(AtlasGenerator generator, AtlasState state) {
        for (AtlasCell cell : state.claims().keySet()) {
            if (state.isClaimed(cell) && generator.isTerritoryless(cell)) {
                AtlasTerritoryTransfer.vacate(generator, state, cell);
            }
        }
    }
}
