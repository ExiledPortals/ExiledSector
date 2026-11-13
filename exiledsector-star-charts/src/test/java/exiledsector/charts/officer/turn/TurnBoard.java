package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.OfficerSeats;
import exiledsector.charts.officer.StartingItemRoll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

final class TurnBoard {

    static final long SEED = 0x51e7L;
    static final int GRID_RADIUS = 6;
    static final TurnRules ATTACKER_WINS = new TurnRules(4, 6f, 100f, 0f, 0f, 1, 3, 2, 0, 50, 6, Map.of());
    static final TurnRules DEFENDER_WINS = new TurnRules(4, 6f, 0f, 0f, 0f, 1, 3, 2, 0, 50, 6, Map.of());

    private TurnBoard() {
    }

    record GridTerrain(AtlasState state) implements TurnTerrain {

        @Override
        public Collection<AtlasCell> lanes(AtlasCell cell) {
            List<AtlasCell> lanes = new ArrayList<>();
            for (AtlasCell lane : List.of(new AtlasCell(cell.gridX() + 1, cell.gridY()), new AtlasCell(cell.gridX() - 1, cell.gridY()),
                    new AtlasCell(cell.gridX(), cell.gridY() + 1), new AtlasCell(cell.gridX(), cell.gridY() - 1))) {
                if (onGrid(lane)) {
                    lanes.add(lane);
                }
            }
            return lanes;
        }

        @Override
        public boolean canClaim(AtlasCell cell) {
            return onGrid(cell) && !state.isClaimed(cell);
        }

        private static boolean onGrid(AtlasCell cell) {
            return Math.abs(cell.gridX()) <= GRID_RADIUS && Math.abs(cell.gridY()) <= GRID_RADIUS;
        }
    }

    static AtlasOfficer place(AtlasState state, int recordId, String factionId, String rankId, AtlasCell centre, AtlasCell... claims) {
        AtlasOfficer officer = AtlasOfficer.waiting(recordId, factionId, rankId, "steady",
                        new AtlasOfficer.OfficerSeeds(recordId, recordId + 1L, recordId + 2L, recordId + 3L), null)
                .seatedAt(rankId, freeSeat(state, rankId))
                .takingCommand(centre, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, centre));
        state.putOfficer(officer);
        state.claim(centre, centre);
        for (AtlasCell claim : claims) {
            state.claim(claim, centre);
        }
        return officer;
    }

    static int freeSeat(AtlasState state, String rankId) {
        int seat = 0;
        while (OfficerSeats.seated(state.officers(), rankId, seat) != null) {
            seat++;
        }
        return seat;
    }

    static TurnSetup setup(TurnRules rules) {
        return setup(rules, IntentResolvers.ABSTRACT, null, Intent.NO_OFFICER);
    }

    static TurnSetup setup(TurnRules rules, IntentResolvers resolvers, AtlasCell openRunCell, int openRunOfficerId) {
        return new TurnSetup(rules, OfficerRanks.all(), resolvers, openRunCell, openRunOfficerId, FlagshipCatalogue.NONE);
    }

    static TurnSetup setup(TurnRules rules, StartingItemRoll startingItems) {
        return new TurnSetup(rules, OfficerRanks.all(), IntentResolvers.ABSTRACT, null, Intent.NO_OFFICER, FlagshipCatalogue.NONE, startingItems);
    }

    static TurnResult take(AtlasState state, TurnCause cause, TurnSetup setup) {
        return OfficerTurn.take(new GridTerrain(state), state, cause, setup);
    }

    static void declare(AtlasState state, Intent... intents) {
        state.setDeclaredIntents(Stream.of(intents).map(Intent::encode).toList());
    }

    static void update(AtlasState state, int recordId, UnaryOperator<AtlasOfficer> change) {
        state.putOfficer(change.apply(state.officer(recordId)));
    }
}
