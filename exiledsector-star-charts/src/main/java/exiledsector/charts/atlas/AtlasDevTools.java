package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AtlasDevTools {

    public static final int SITE_SEARCH_RADIUS = 40;
    static final int PATH_SEARCH_LIMIT = 40000;

    public record Site(AtlasCell cell, AtlasSpecial special) {
    }

    private AtlasDevTools() {
    }

    public static List<Site> sites(AtlasGenerator generator, int radius) {
        List<Site> sites = new ArrayList<>();
        for (AtlasCell cell : AtlasCell.ORIGIN.around(radius)) {
            AtlasSpecial special = generator.specialAt(cell);
            if (special != null) {
                sites.add(new Site(cell, special));
            }
        }
        sites.sort(Comparator.comparingDouble((Site site) -> site.cell().centreDistance())
                .thenComparingInt(site -> site.cell().gridX()).thenComparingInt(site -> site.cell().gridY()));
        return sites;
    }

    public static AtlasCell nearestOpenSite(AtlasGenerator generator, AtlasState state, String specialId) {
        for (Site site : sites(generator, SITE_SEARCH_RADIUS)) {
            if (site.special().id().equalsIgnoreCase(specialId) && !state.isCompleted(site.cell())
                    && !AtlasReach.isSpentBastion(generator, state, site.cell())) {
                return site.cell();
            }
        }
        return null;
    }

    public static List<AtlasCell> pathTo(AtlasGenerator generator, AtlasState state, AtlasCell target) {
        if (generator.isVoid(target)) {
            return null;
        }
        Map<AtlasCell, AtlasCell> cameFrom = new HashMap<>();
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>();
        for (AtlasCell completed : state.completedCells()) {
            cameFrom.put(completed, null);
            frontier.add(completed);
        }
        while (!frontier.isEmpty() && cameFrom.size() < PATH_SEARCH_LIMIT) {
            AtlasCell cell = frontier.poll();
            for (AtlasCell next : generator.lanes(cell)) {
                if (cameFrom.containsKey(next)) {
                    continue;
                }
                cameFrom.put(next, cell);
                if (next.equals(target)) {
                    return between(cameFrom, target);
                }
                if (generator.specialAt(next) == null) {
                    frontier.add(next);
                }
            }
        }
        return cameFrom.containsKey(target) ? List.of() : null;
    }

    private static List<AtlasCell> between(Map<AtlasCell, AtlasCell> cameFrom, AtlasCell target) {
        List<AtlasCell> path = new ArrayList<>();
        for (AtlasCell step = cameFrom.get(target); step != null && cameFrom.get(step) != null; step = cameFrom.get(step)) {
            path.add(0, step);
        }
        return path;
    }

    public static List<AtlasCell> reach(AtlasGenerator generator, AtlasState state, AtlasCell target) {
        List<AtlasCell> path = pathTo(generator, state, target);
        if (path == null) {
            return null;
        }
        for (AtlasCell cell : path) {
            state.add(cell, AtlasState.COMPLETED);
        }
        AtlasReach.refreshReveal(generator, state);
        return path;
    }

    public static void complete(AtlasGenerator generator, AtlasState state, AtlasCell cell, int tier) {
        AtlasProgress.complete(generator, state, cell, tier);
        AtlasReach.refreshReveal(generator, state);
    }

    public static void fail(AtlasState state, AtlasCell cell) {
        AtlasProgress.fail(state, cell);
    }

    public static AtlasState reset(SectorAPI sector, Long seed) {
        sector.getPersistentData().remove(AtlasState.PERSISTENT_KEY);
        AtlasState state = seed == null ? AtlasState.get(sector) : new AtlasState(seed);
        sector.getPersistentData().put(AtlasState.PERSISTENT_KEY, state);
        AtlasRuntime.invalidate();
        return state;
    }
}
