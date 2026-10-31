package exiledsector.charts.atlas;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AtlasReach {

    public static final int VISIBLE_LANES = 2;
    public static final int LISTENING_POST_LANES = 3;

    private AtlasReach() {
    }

    public static boolean isReachable(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        if (generator.isVoid(cell) || state.isCompleted(cell) || isSpentBastion(generator, state, cell)) {
            return false;
        }
        for (AtlasCell neighbour : generator.lanes(cell)) {
            if (state.isCompleted(neighbour)) {
                return true;
            }
        }
        return false;
    }

    public static Set<AtlasCell> reachable(AtlasGenerator generator, AtlasState state) {
        Set<AtlasCell> reachable = new LinkedHashSet<>();
        for (AtlasCell completed : state.completedCells()) {
            for (AtlasCell neighbour : generator.lanes(completed)) {
                if (isReachable(generator, state, neighbour)) {
                    reachable.add(neighbour);
                }
            }
        }
        return reachable;
    }

    public static Set<AtlasCell> visible(AtlasGenerator generator, AtlasState state) {
        Map<AtlasCell, Integer> remaining = new HashMap<>();
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>();
        for (AtlasCell completed : state.completedCells()) {
            int reach = Math.max(VISIBLE_LANES, revealLanes(generator, completed));
            if (reach > remaining.getOrDefault(completed, -1)) {
                remaining.put(completed, reach);
                frontier.add(completed);
            }
        }
        for (AtlasCell revealed : state.cellsWith(AtlasState.LANES_REVEALED)) {
            if (!generator.isVoid(revealed) && LISTENING_POST_LANES > remaining.getOrDefault(revealed, -1)) {
                remaining.put(revealed, LISTENING_POST_LANES);
                frontier.add(revealed);
            }
        }
        while (!frontier.isEmpty()) {
            AtlasCell cell = frontier.poll();
            int left = remaining.get(cell);
            if (left <= 0) {
                continue;
            }
            for (AtlasCell neighbour : generator.lanes(cell)) {
                if (left - 1 > remaining.getOrDefault(neighbour, -1)) {
                    remaining.put(neighbour, left - 1);
                    frontier.add(neighbour);
                }
            }
        }
        return new LinkedHashSet<>(remaining.keySet());
    }

    static int revealLanes(AtlasGenerator generator, AtlasCell completed) {
        AtlasSpecial special = generator.specialAt(completed);
        return special != null && special.kind().revealsLanes() ? Math.round(special.radius()) : 0;
    }

    public static boolean isSpentBastion(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasSpecial special = generator.specialAt(cell);
        return special != null && special.kind().oneAttempt() && state.has(cell, AtlasState.ATTEMPTED);
    }

    public static Set<AtlasCell> refreshReveal(AtlasGenerator generator, AtlasState state) {
        Set<AtlasCell> visible = visible(generator, state);
        state.recordReveal(generator, visible);
        return visible;
    }
}
