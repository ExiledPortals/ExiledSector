package exiledsector.skills.skilleffect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class WingScramble {

    record Move<F, W>(F fighter, W toWing) {
    }

    record Spawn<W>(W hostWing, W donorWing) {
    }

    record Routing<F, W>(List<Move<F, W>> moves, List<F> discards, List<Spawn<W>> spawns) {
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

    static <F, W> Routing<F, W> route(List<W> group, Map<W, List<F>> membersByWing, Map<F, W> donorByFighter, int copiesPerDonor,
                                      boolean spawnMissing) {
        Map<W, Map<W, List<F>>> holdingsByHost = new IdentityHashMap<>();
        Map<W, Deque<F>> surplusByDonor = new IdentityHashMap<>();
        for (W host : group) {
            Map<W, List<F>> holdings = new IdentityHashMap<>();
            for (F fighter : membersByWing.getOrDefault(host, List.of())) {
                W donor = donorByFighter.getOrDefault(fighter, host);
                holdings.computeIfAbsent(donor, ignored -> new ArrayList<>()).add(fighter);
            }
            holdingsByHost.put(host, holdings);
            for (Map.Entry<W, List<F>> holding : holdings.entrySet()) {
                List<F> held = holding.getValue();
                int wanted = wanted(host, holding.getKey(), group, copiesPerDonor);
                for (int index = wanted; index < held.size(); index++) {
                    surplusByDonor.computeIfAbsent(holding.getKey(), ignored -> new ArrayDeque<>()).add(held.get(index));
                }
            }
        }
        List<Move<F, W>> moves = new ArrayList<>();
        List<Spawn<W>> spawns = new ArrayList<>();
        for (W host : group) {
            for (W donor : group) {
                int missing = wanted(host, donor, group, copiesPerDonor) - holdingsByHost.get(host).getOrDefault(donor, List.of()).size();
                Deque<F> surplus = surplusByDonor.getOrDefault(donor, new ArrayDeque<>());
                for (int slot = 0; slot < missing; slot++) {
                    if (!surplus.isEmpty()) {
                        moves.add(new Move<>(surplus.poll(), host));
                    } else if (spawnMissing && donor != host) {
                        spawns.add(new Spawn<>(host, donor));
                    }
                }
            }
        }
        List<F> discards = new ArrayList<>();
        surplusByDonor.values().forEach(discards::addAll);
        return new Routing<>(moves, discards, spawns);
    }

    private static <W> int wanted(W host, W donor, List<W> group, int copiesPerDonor) {
        if (!group.contains(donor)) {
            return 0;
        }
        return donor == host ? 1 : copiesPerDonor;
    }
}
