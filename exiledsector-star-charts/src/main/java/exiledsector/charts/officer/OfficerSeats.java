package exiledsector.charts.officer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class OfficerSeats {

    private OfficerSeats() {
    }

    public static int superiorSeat(List<OfficerRank> ranks, int rankOrder, int seat) {
        if (seat < 0 || rankOrder < 0 || rankOrder + 1 >= ranks.size()) {
            return AtlasOfficer.UNSEATED;
        }
        int places = Math.max(1, ranks.get(rankOrder).places());
        return Math.min(seat, places - 1) * ranks.get(rankOrder + 1).places() / places;
    }

    public static List<Integer> subordinateSeats(List<OfficerRank> ranks, int rankOrder, int seat) {
        List<Integer> seats = new ArrayList<>();
        if (seat < 0 || rankOrder <= 0 || rankOrder >= ranks.size()) {
            return seats;
        }
        for (int lowerSeat = 0; lowerSeat < ranks.get(rankOrder - 1).places(); lowerSeat++) {
            if (superiorSeat(ranks, rankOrder - 1, lowerSeat) == seat) {
                seats.add(lowerSeat);
            }
        }
        return seats;
    }

    public static AtlasOfficer superior(List<OfficerRank> ranks, Collection<AtlasOfficer> officers, AtlasOfficer officer) {
        int rankOrder = order(ranks, officer.rankId());
        int seat = superiorSeat(ranks, rankOrder, officer.seat());
        return seat < 0 ? null : seated(officers, ranks.get(rankOrder + 1).id(), seat);
    }

    public static List<AtlasOfficer> subordinates(List<OfficerRank> ranks, Collection<AtlasOfficer> officers, AtlasOfficer officer) {
        int rankOrder = order(ranks, officer.rankId());
        List<AtlasOfficer> found = new ArrayList<>();
        for (int seat : subordinateSeats(ranks, rankOrder, officer.seat())) {
            AtlasOfficer subordinate = seated(officers, ranks.get(rankOrder - 1).id(), seat);
            if (subordinate != null) {
                found.add(subordinate);
            }
        }
        return found;
    }

    public static boolean adjacent(List<OfficerRank> ranks, Collection<AtlasOfficer> officers, AtlasOfficer first, AtlasOfficer second) {
        AtlasOfficer firstSuperior = superior(ranks, officers, first);
        AtlasOfficer secondSuperior = superior(ranks, officers, second);
        return firstSuperior != null && firstSuperior.recordId() == second.recordId()
                || secondSuperior != null && secondSuperior.recordId() == first.recordId();
    }

    public static AtlasOfficer seated(Collection<AtlasOfficer> officers, String rankId, int seat) {
        for (AtlasOfficer officer : officers) {
            if (officer.seat() == seat && officer.rankId().equals(rankId)) {
                return officer;
            }
        }
        return null;
    }

    public static int order(List<OfficerRank> ranks, String rankId) {
        for (int index = 0; index < ranks.size(); index++) {
            if (ranks.get(index).id().equals(rankId)) {
                return index;
            }
        }
        return 0;
    }
}
