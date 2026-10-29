package exiledsector.skills.skilleffect.fighter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class WingScramble {

    record Host<F, W, K>(W wing, K type, int wingSize, int extraAllowance, List<F> members) {
    }

    record Move<F, W>(F fighter, W toWing) {
    }

    record Conversion<F, W, K>(F fighter, W hostWing, K fighterType) {
    }

    record Routing<F, W, K>(List<Move<F, W>> moves, List<Conversion<F, W, K>> conversions, List<F> discards) {
    }

    private record Spare<F, K>(F fighter, K type, int ownerIndex) {
    }

    private WingScramble() {
    }

    static <W, G> List<List<W>> groups(List<W> wings, Function<W, G> groupOf) {
        Map<G, List<W>> wingsByGroup = new LinkedHashMap<>();
        for (W wing : wings) {
            G group = groupOf.apply(wing);
            if (group != null) {
                wingsByGroup.computeIfAbsent(group, ignored -> new ArrayList<>()).add(wing);
            }
        }
        return wingsByGroup.values().stream().filter(group -> group.size() > 1).toList();
    }

    static <K> Map<K, Integer> composition(K hostType, List<K> groupTypes, int sameTypeOrdinal, int wingSize, int copiesPerType) {
        Map<K, Integer> wanted = new LinkedHashMap<>();
        if (wingSize <= 0) {
            return wanted;
        }
        wanted.put(hostType, 1);
        List<K> otherTypes = rotatedOtherTypes(hostType, groupTypes, sameTypeOrdinal);
        int openPlaces = wingSize - 1;
        for (int round = 0; round < copiesPerType && openPlaces > 0 && !otherTypes.isEmpty(); round++) {
            for (int index = 0; index < otherTypes.size() && openPlaces > 0; index++) {
                wanted.merge(otherTypes.get(index), 1, Integer::sum);
                openPlaces--;
            }
        }
        wanted.merge(hostType, openPlaces, Integer::sum);
        return wanted;
    }

    private static <K> List<K> rotatedOtherTypes(K hostType, List<K> groupTypes, int sameTypeOrdinal) {
        int hostTypeIndex = groupTypes.indexOf(hostType);
        List<K> otherTypes = new ArrayList<>(groupTypes.size());
        for (int step = 1; step < groupTypes.size(); step++) {
            K candidate = groupTypes.get(Math.floorMod(hostTypeIndex + step, groupTypes.size()));
            if (!candidate.equals(hostType)) {
                otherTypes.add(candidate);
            }
        }
        if (!otherTypes.isEmpty()) {
            Collections.rotate(otherTypes, -(sameTypeOrdinal % otherTypes.size()));
        }
        return otherTypes;
    }

    static <F, W, K> Routing<F, W, K> route(List<Host<F, W, K>> hosts, Map<F, K> typeByFighter, Predicate<F> pinned, int copiesPerType) {
        Set<K> distinctTypes = new LinkedHashSet<>();
        for (Host<F, W, K> host : hosts) {
            distinctTypes.add(host.type());
        }
        List<K> groupTypes = new ArrayList<>(distinctTypes);
        Map<K, Integer> hostsSeenByType = new HashMap<>();
        List<Map<K, Integer>> openPlacesByHost = new ArrayList<>(hosts.size());
        List<Spare<F, K>> spares = new ArrayList<>();
        for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
            Host<F, W, K> host = hosts.get(hostIndex);
            int sameTypeOrdinal = hostsSeenByType.merge(host.type(), 1, Integer::sum) - 1;
            Map<K, Integer> wanted = composition(host.type(), groupTypes, sameTypeOrdinal, host.wingSize(), copiesPerType);
            openPlacesByHost.add(collectSpares(host, hostIndex, wanted, typeByFighter, pinned, spares));
        }
        boolean[] claimed = new boolean[spares.size()];
        List<Move<F, W>> moves = new ArrayList<>();
        for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
            for (Map.Entry<K, Integer> openPlaces : openPlacesByHost.get(hostIndex).entrySet()) {
                int stillOpen = openPlaces.getValue();
                for (int spareIndex = 0; spareIndex < spares.size() && stillOpen > 0; spareIndex++) {
                    if (!claimed[spareIndex] && spares.get(spareIndex).type().equals(openPlaces.getKey())) {
                        claimed[spareIndex] = true;
                        moves.add(new Move<>(spares.get(spareIndex).fighter(), hosts.get(hostIndex).wing()));
                        stillOpen--;
                    }
                }
                openPlaces.setValue(stillOpen);
            }
        }
        List<Conversion<F, W, K>> conversions = new ArrayList<>();
        for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
            for (Map.Entry<K, Integer> openPlaces : openPlacesByHost.get(hostIndex).entrySet()) {
                for (int place = 0; place < openPlaces.getValue(); place++) {
                    int spareIndex = conversionSpare(spares, claimed, hostIndex);
                    if (spareIndex < 0) {
                        break;
                    }
                    claimed[spareIndex] = true;
                    conversions.add(new Conversion<>(spares.get(spareIndex).fighter(), hosts.get(hostIndex).wing(), openPlaces.getKey()));
                }
            }
        }
        List<F> discards = new ArrayList<>();
        for (int spareIndex = 0; spareIndex < spares.size(); spareIndex++) {
            if (!claimed[spareIndex]) {
                discards.add(spares.get(spareIndex).fighter());
            }
        }
        return new Routing<>(moves, conversions, discards);
    }

    private static <F, W, K> Map<K, Integer> collectSpares(Host<F, W, K> host, int hostIndex, Map<K, Integer> wanted, Map<F, K> typeByFighter,
                                                           Predicate<F> pinned, List<Spare<F, K>> spares) {
        Map<K, List<F>> heldByType = new LinkedHashMap<>();
        Map<K, Integer> pinnedByType = new HashMap<>();
        for (F fighter : host.members()) {
            K fighterType = typeByFighter.getOrDefault(fighter, host.type());
            if (pinned.test(fighter)) {
                pinnedByType.merge(fighterType, 1, Integer::sum);
            } else {
                heldByType.computeIfAbsent(fighterType, ignored -> new ArrayList<>()).add(fighter);
            }
        }
        Map<K, Integer> openPlaces = new LinkedHashMap<>();
        int totalOpenPlaces = 0;
        for (Map.Entry<K, Integer> wantedType : wanted.entrySet()) {
            int stillWanted = wantedType.getValue() - pinnedByType.getOrDefault(wantedType.getKey(), 0)
                    - heldByType.getOrDefault(wantedType.getKey(), List.of()).size();
            if (stillWanted > 0) {
                openPlaces.put(wantedType.getKey(), stillWanted);
                totalOpenPlaces += stillWanted;
            }
        }
        List<F> spareNatives = new ArrayList<>();
        for (Map.Entry<K, List<F>> holding : heldByType.entrySet()) {
            int kept = Math.max(0, wanted.getOrDefault(holding.getKey(), 0) - pinnedByType.getOrDefault(holding.getKey(), 0));
            List<F> held = holding.getValue();
            for (int index = kept; index < held.size(); index++) {
                if (holding.getKey().equals(host.type())) {
                    spareNatives.add(held.get(index));
                } else {
                    spares.add(new Spare<>(held.get(index), holding.getKey(), hostIndex));
                }
            }
        }
        int extrasKept = Math.min(host.extraAllowance(), Math.max(0, spareNatives.size() - totalOpenPlaces));
        for (int index = 0; index < spareNatives.size() - extrasKept; index++) {
            spares.add(new Spare<>(spareNatives.get(index), host.type(), hostIndex));
        }
        return openPlaces;
    }

    private static <F, K> int conversionSpare(List<Spare<F, K>> spares, boolean[] claimed, int hostIndex) {
        int fromAnotherHost = -1;
        for (int spareIndex = 0; spareIndex < spares.size(); spareIndex++) {
            if (claimed[spareIndex]) {
                continue;
            }
            if (spares.get(spareIndex).ownerIndex() == hostIndex) {
                return spareIndex;
            }
            if (fromAnotherHost < 0) {
                fromAnotherHost = spareIndex;
            }
        }
        return fromAnotherHost;
    }
}
