package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerSeats;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import exiledsector.charts.officer.turn.OfficerTurn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OfficerSeatRepair {

    private static final long FILL_FACTION_SALT = 0x4f53454154L;

    private OfficerSeatRepair() {
    }

    public static boolean repair(AtlasGenerator generator, AtlasState state, List<OfficerRank> ranks, TerritoryContext context) {
        List<AtlasOfficer> officers = state.officers();
        if (officers.isEmpty() || everySeatHeldOnce(ranks, officers)) {
            return false;
        }
        int officerCount = officers.size();
        Comparator<AtlasOfficer> keepFirst = keepOrder(state, context);
        Set<Integer> seatedIds = validlySeated(ranks, officers);
        boolean anyUnseated = seatedIds.size() < officerCount;
        Set<String> takenSeats = new HashSet<>();
        officers.stream().filter(officer -> seatedIds.contains(officer.recordId()))
                .forEach(officer -> takenSeats.add(seatKey(officer.rankId(), officer.seat())));
        List<AtlasOfficer> unplaced = new ArrayList<>();
        for (int rankOrder = ranks.size() - 1; rankOrder >= 0; rankOrder--) {
            OfficerRank rank = ranks.get(rankOrder);
            for (AtlasOfficer officer : officers) {
                if (!seatedIds.contains(officer.recordId()) && OfficerSeats.order(ranks, officer.rankId()) == rankOrder) {
                    unplaced.add(officer);
                }
            }
            unplaced.sort(keepFirst);
            for (int seat = 0; seat < rank.places(); seat++) {
                if (takenSeats.add(seatKey(rank.id(), seat))) {
                    seatOrFill(generator, state, ranks, context, rankOrder, seat, unplaced);
                }
            }
        }
        unplaced.forEach(officer -> dismiss(state, state.officer(officer.recordId())));
        if (!anyUnseated && state.officers().size() == officerCount) {
            return false;
        }
        state.setLastTurnResult(List.of());
        state.setDeclaredIntents(OfficerTurn.decodeIntents(state.declaredIntents()).stream()
                .filter(intent -> intent.kind() != IntentKind.CHALLENGE).map(Intent::encode).toList());
        return true;
    }

    private static void seatOrFill(AtlasGenerator generator, AtlasState state, List<OfficerRank> ranks, TerritoryContext context, int rankOrder,
                                   int seat, List<AtlasOfficer> unplaced) {
        String rankId = ranks.get(rankOrder).id();
        if (!unplaced.isEmpty()) {
            AtlasOfficer officer = unplaced.remove(0);
            state.putOfficer(officer.seatedAt(rankId, seat));
            return;
        }
        String factionId = AtlasHash.race(state.seed(), FILL_FACTION_SALT, rankOrder, seat, fillFactions(generator, context));
        if (factionId == null) {
            return;
        }
        state.putOfficer(OfficerBoardGenerator.atRank(state.seed(), state.newOfficerRecordId(), factionId, rankId, seat, ranks,
                context.catalogue(), context.startingItems()));
    }

    private static Map<String, Float> fillFactions(AtlasGenerator generator, TerritoryContext context) {
        Map<String, Float> factions = new LinkedHashMap<>(TerritoryFactions.boardFactions(generator.rules()));
        if (context.excludedFactionId() != null && factions.size() > 1) {
            factions.remove(context.excludedFactionId());
        }
        return factions;
    }

    private static void dismiss(AtlasState state, AtlasOfficer officer) {
        if (officer.isActive()) {
            AtlasTerritoryTransfer.dismissOfficer(state, officer.commandCentre());
        }
        state.leaveBehind(officer.carried());
        state.removeOfficer(officer.recordId());
    }

    private static String seatKey(String rankId, int seat) {
        return rankId + "#" + seat;
    }

    static boolean everySeatHeldOnce(List<OfficerRank> ranks, List<AtlasOfficer> officers) {
        int seatCount = ranks.stream().mapToInt(OfficerRank::places).sum();
        return officers.size() == seatCount && validlySeated(ranks, officers).size() == seatCount;
    }

    private static Set<Integer> validlySeated(List<OfficerRank> ranks, List<AtlasOfficer> officers) {
        Set<String> taken = new HashSet<>();
        Set<Integer> seated = new HashSet<>();
        officers.stream().sorted(Comparator.comparingInt(AtlasOfficer::recordId)).forEach(officer -> {
            OfficerRank rank = ranks.stream().filter(candidate -> candidate.id().equals(officer.rankId())).findFirst().orElse(null);
            if (rank != null && officer.seat() >= 0 && officer.seat() < rank.places() && taken.add(seatKey(rank.id(), officer.seat()))) {
                seated.add(officer.recordId());
            }
        });
        return seated;
    }

    private static Comparator<AtlasOfficer> keepOrder(AtlasState state, TerritoryContext context) {
        return Comparator.<AtlasOfficer>comparingInt(officer -> context.takesPartInOpenRun(state, officer) ? 0 : 1)
                .thenComparingInt(officer -> officer.isActive() ? 0 : officer.hasFled() ? 1 : 2)
                .thenComparingInt(officer -> officer.hasPendingChoice() ? 0 : 1)
                .thenComparing(Comparator.comparingInt(AtlasOfficer::renown).reversed())
                .thenComparingInt(AtlasOfficer::recordId);
    }
}
