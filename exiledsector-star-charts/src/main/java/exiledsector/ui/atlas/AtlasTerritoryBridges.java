package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

final class AtlasTerritoryBridges {

    record Bridges(Map<Long, String> cells, Map<Long, String> corners) {
    }

    private AtlasTerritoryBridges() {
    }

    static Bridges bridges(Collection<AtlasCell> systems, Function<AtlasCell, String> territoryOf,
                           Function<AtlasCell, ? extends Collection<AtlasCell>> lanes, Predicate<AtlasCell> hasSystem) {
        Set<Long> systemCells = new HashSet<>();
        systems.forEach(cell -> systemCells.add(cell.pack()));
        List<AtlasCell> ordered = new ArrayList<>(systems);
        ordered.sort(Comparator.comparingLong(AtlasCell::pack));
        Map<Long, Map<String, Integer>> cellCrossings = new HashMap<>();
        Map<Long, Map<String, Integer>> cornerCrossings = new HashMap<>();
        for (AtlasCell from : ordered) {
            String territoryId = territoryOf.apply(from);
            if (territoryId == null) {
                continue;
            }
            Predicate<AtlasCell> open = cell -> !hasSystem.test(cell) || territoryId.equals(territoryOf.apply(cell));
            for (AtlasCell to : lanes.apply(from)) {
                if (!systemCells.contains(to.pack()) || !territoryId.equals(territoryOf.apply(to))) {
                    continue;
                }
                List<AtlasCell> path = path(from, to, open);
                for (int step = 1; step < path.size(); step++) {
                    AtlasCell previous = path.get(step - 1);
                    AtlasCell here = path.get(step);
                    if (previous.gridX() != here.gridX() && previous.gridY() != here.gridY()) {
                        count(cornerCrossings, AtlasTerritoryBorders.cornerBetween(previous, here), territoryId);
                    }
                    if (!hasSystem.test(here)) {
                        count(cellCrossings, here.pack(), territoryId);
                    }
                }
            }
        }
        return new Bridges(mostCrossed(cellCrossings), mostCrossed(cornerCrossings));
    }

    static List<AtlasCell> path(AtlasCell from, AtlasCell to, Predicate<AtlasCell> open) {
        int spanX = Math.abs(to.gridX() - from.gridX());
        int spanY = Math.abs(to.gridY() - from.gridY());
        int stepX = Integer.signum(to.gridX() - from.gridX());
        int stepY = Integer.signum(to.gridY() - from.gridY());
        int x = from.gridX();
        int y = from.gridY();
        List<AtlasCell> path = new ArrayList<>(List.of(from));
        for (int movedX = 0, movedY = 0; movedX < spanX || movedY < spanY; ) {
            long lead = (1L + 2L * movedX) * spanY - (1L + 2L * movedY) * spanX;
            boolean alongX = lead < 0;
            boolean alongY = lead > 0;
            if (lead == 0) {
                alongX = open.test(new AtlasCell(x + stepX, y));
                alongY = !alongX && open.test(new AtlasCell(x, y + stepY));
                if (!alongX && !alongY) {
                    alongX = true;
                    alongY = true;
                }
            }
            if (alongX) {
                x += stepX;
                movedX++;
            }
            if (alongY) {
                y += stepY;
                movedY++;
            }
            path.add(new AtlasCell(x, y));
        }
        return path;
    }

    private static void count(Map<Long, Map<String, Integer>> crossings, long key, String territoryId) {
        crossings.computeIfAbsent(key, packed -> new TreeMap<>()).merge(territoryId, 1, Integer::sum);
    }

    private static Map<Long, String> mostCrossed(Map<Long, Map<String, Integer>> crossings) {
        Map<Long, String> most = new HashMap<>();
        crossings.forEach((key, counts) -> {
            String mostId = null;
            int mostCount = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > mostCount) {
                    mostId = entry.getKey();
                    mostCount = entry.getValue();
                }
            }
            most.put(key, mostId);
        });
        return most;
    }
}
