package exiledsector.skills.skilleffect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

final class WingScramble {

    record Move<F, W>(F fighter, W toWing) {
    }

    record Spawn<W, K>(W hostWing, K fighterType) {
    }

    record Routing<F, W, K>(List<Move<F, W>> moves, List<F> discards, List<Spawn<W, K>> spawns) {
    }

    private WingScramble() {
    }

    static <W, R> List<List<W>> roleGroups(List<W> wings, Function<W, R> roleOf) {
        Map<R, List<W>> wingsByRole = new LinkedHashMap<>();
        for (W wing : wings) {
            R role = roleOf.apply(wing);
            if (role != null) {
                wingsByRole.computeIfAbsent(role, ignored -> new ArrayList<>()).add(wing);
            }
        }
        return wingsByRole.values().stream().filter(group -> group.size() > 1).toList();
    }

    static <F, W, K> Routing<F, W, K> route(List<W> group, Map<W, List<F>> membersByWing, Map<F, K> typeByFighter, Function<W, K> typeOf,
                                            int copiesPerType, boolean spawnMissing) {
        Set<K> groupTypes = new LinkedHashSet<>();
        for (W wing : group) {
            groupTypes.add(typeOf.apply(wing));
        }
        Map<W, Map<K, List<F>>> holdingsByHost = new IdentityHashMap<>();
        Map<K, Deque<F>> surplusByType = new LinkedHashMap<>();
        for (W host : group) {
            K hostType = typeOf.apply(host);
            Map<K, List<F>> holdings = new LinkedHashMap<>();
            for (F fighter : membersByWing.getOrDefault(host, List.of())) {
                K fighterType = typeByFighter.getOrDefault(fighter, hostType);
                holdings.computeIfAbsent(fighterType, ignored -> new ArrayList<>()).add(fighter);
            }
            holdingsByHost.put(host, holdings);
            for (Map.Entry<K, List<F>> holding : holdings.entrySet()) {
                List<F> held = holding.getValue();
                int wanted = wanted(hostType, holding.getKey(), groupTypes, copiesPerType);
                for (int index = wanted; index < held.size(); index++) {
                    surplusByType.computeIfAbsent(holding.getKey(), ignored -> new ArrayDeque<>()).add(held.get(index));
                }
            }
        }
        List<Move<F, W>> moves = new ArrayList<>();
        List<Spawn<W, K>> spawns = new ArrayList<>();
        for (W host : group) {
            K hostType = typeOf.apply(host);
            for (K fighterType : groupTypes) {
                int missing = wanted(hostType, fighterType, groupTypes, copiesPerType)
                        - holdingsByHost.get(host).getOrDefault(fighterType, List.of()).size();
                Deque<F> surplus = surplusByType.getOrDefault(fighterType, new ArrayDeque<>());
                for (int slot = 0; slot < missing; slot++) {
                    if (!surplus.isEmpty()) {
                        moves.add(new Move<>(surplus.poll(), host));
                    } else if (spawnMissing && !fighterType.equals(hostType)) {
                        spawns.add(new Spawn<>(host, fighterType));
                    }
                }
            }
        }
        List<F> discards = new ArrayList<>();
        surplusByType.values().forEach(discards::addAll);
        return new Routing<>(moves, discards, spawns);
    }

    private static <K> int wanted(K hostType, K fighterType, Set<K> groupTypes, int copiesPerType) {
        if (!groupTypes.contains(fighterType)) {
            return 0;
        }
        return fighterType.equals(hostType) ? 1 : copiesPerType;
    }
}
