package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.OfficerText;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

public final class AtlasTerritoryPlacer {

    static final int CENTRE_SPACING_LANES = 3;
    static final int UNCLAIMED_GAP_LANES = 2;
    static final int MIN_STARTING_CLAIM = 4;
    static final int MAX_STARTING_CLAIM = 10;
    static final int MAX_VISIBLE_TERRITORIES = 3;
    static final int FLED_REACH_LANES = 3;
    static final float FLED_WEIGHT = 10f;
    static final float ABSENT_WEIGHT = 4f;
    public static final String ARRIVED_HISTORY = "arrived";
    public static final String MOVED_HISTORY = "moved";
    public static final String FLED_HISTORY = "fled";
    public static final String HISTORY_SEPARATOR = ":";
    private static final long CANDIDATE_SALT = 0x4343454e5452L;
    private static final long CLAIM_SIZE_SALT = 0x43534953455aL;
    private static final long OFFICER_SALT = 0x434f464649L;

    private final AtlasGenerator generator;
    private final AtlasState state;
    private final Set<AtlasCell> visible;
    private final Set<AtlasCell> reachable;
    private final TerritoryContext context;

    private AtlasTerritoryPlacer(AtlasGenerator generator, AtlasState state, Set<AtlasCell> visible, TerritoryContext context) {
        this.generator = generator;
        this.state = state;
        this.visible = visible;
        this.reachable = AtlasReach.reachable(generator, state);
        this.context = context;
    }

    public static void place(AtlasGenerator generator, AtlasState state, Set<AtlasCell> visible, TerritoryContext context) {
        fillBoard(generator, state, context);
        OfficerSeatRepair.repair(generator, state, OfficerRanks.all(), context);
        AtlasTerritoryPlacer placer = new AtlasTerritoryPlacer(generator, state, visible, context);
        placer.placeInNewChunks();
        if (state.fillFingerprintChanged(fillFingerprint(visible, state))) {
            placer.fillGaps();
            state.fillFingerprintChanged(fillFingerprint(visible, state));
        }
    }

    private static long fillFingerprint(Set<AtlasCell> visible, AtlasState state) {
        return Objects.hash(visible, state.claims(), state.officers());
    }

    static void fillBoard(AtlasGenerator generator, AtlasState state, TerritoryContext context) {
        if (!state.officers().isEmpty()) {
            return;
        }
        OfficerBoardGenerator.generate(state.seed(), OfficerRanks.all(), TerritoryFactions.boardFactions(generator.rules()), context.catalogue(),
                        context.startingItems())
                .forEach(state::putOfficer);
    }

    public static String history(String event, AtlasCell... cells) {
        StringBuilder entry = new StringBuilder(event);
        for (AtlasCell cell : cells) {
            entry.append(HISTORY_SEPARATOR).append(cell.pack());
        }
        return entry.toString();
    }

    private void placeInNewChunks() {
        Set<Long> chunkKeys = new TreeSet<>();
        visible.forEach(cell -> chunkKeys.add(AtlasGenerator.chunkKey(cell.chunkX(), cell.chunkY())));
        for (long chunkKey : chunkKeys) {
            if (state.decideChunk(chunkKey)) {
                placeInChunk(AtlasCell.unpack(chunkKey));
            }
        }
    }

    private void placeInChunk(AtlasCell chunk) {
        List<AtlasCell> candidates = AtlasSpecialPlacer.cellsOf(chunk.gridX(), chunk.gridY()).stream()
                .sorted(Comparator.comparingLong(cell -> AtlasHash.of(generator.seed(), CANDIDATE_SALT, cell.gridX(), cell.gridY())))
                .toList();
        for (AtlasCell candidate : candidates) {
            if (canHoldTerritory(candidate) && !hasCentreWithin(candidate, CENTRE_SPACING_LANES - 1)) {
                placeCentre(candidate);
                return;
            }
        }
    }

    private void fillGaps() {
        Set<AtlasCell> ordered = new TreeSet<>(Comparator.comparingLong(AtlasCell::pack));
        ordered.addAll(visible);
        for (AtlasCell cell : ordered) {
            if (canHoldTerritory(cell) && !hasClaimWithin(cell, UNCLAIMED_GAP_LANES)) {
                placeCentre(cell);
            }
        }
    }

    private boolean canHoldTerritory(AtlasCell cell) {
        return canHoldTerritory(generator, state, cell, context.openRunCell());
    }

    public static boolean canHoldTerritory(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasCell openRunCell) {
        if (cell.equals(AtlasCell.ORIGIN) || cell.equals(openRunCell) || generator.isVoid(cell) || state.isCompleted(cell)
                || state.isClaimed(cell) || generator.specialAt(cell) != null) {
            return false;
        }
        return generator.infestationAt(cell) == null || state.has(cell, AtlasState.INFESTATION_CLEARED);
    }

    private boolean hasCentreWithin(AtlasCell cell, int lanes) {
        return withinLanes(cell, lanes).stream().anyMatch(near -> state.officerAt(near) != null);
    }

    private boolean hasClaimWithin(AtlasCell cell, int lanes) {
        return withinLanes(cell, lanes).stream().anyMatch(state::isClaimed);
    }

    private Set<AtlasCell> withinLanes(AtlasCell start, int lanes) {
        Map<AtlasCell, Integer> distances = new LinkedHashMap<>();
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>();
        distances.put(start, 0);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            AtlasCell cell = frontier.poll();
            int distance = distances.get(cell);
            if (distance >= lanes) {
                continue;
            }
            for (AtlasCell neighbour : generator.lanes(cell)) {
                if (distances.putIfAbsent(neighbour, distance + 1) == null) {
                    frontier.add(neighbour);
                }
            }
        }
        return distances.keySet();
    }

    private void placeCentre(AtlasCell centre) {
        AtlasOfficer officer = pickOfficer(centre);
        if (officer == null) {
            return;
        }
        if (officer.isActive()) {
            AtlasCell previous = officer.commandCentre();
            state.breakUp(previous);
            officer = officer.takingCommand(centre, history(MOVED_HISTORY, previous, centre));
        } else if (OfficerText.fledFrom(officer) != null) {
            officer = officer.takingCommand(centre, history(MOVED_HISTORY, OfficerText.fledFrom(officer), centre));
        } else {
            officer = state.withLeftBehindItems(officer.takingCommand(centre, history(ARRIVED_HISTORY, centre)));
        }
        state.putOfficer(officer);
        claimStartingTerritory(centre);
    }

    private void claimStartingTerritory(AtlasCell centre) {
        long sizeHash = AtlasHash.of(generator.seed(), CLAIM_SIZE_SALT, centre.gridX(), centre.gridY());
        int size = MIN_STARTING_CLAIM + (int) Math.floorMod(sizeHash, (long) MAX_STARTING_CLAIM - MIN_STARTING_CLAIM + 1);
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>();
        Set<AtlasCell> seen = new HashSet<>();
        frontier.add(centre);
        seen.add(centre);
        int claimed = 0;
        while (!frontier.isEmpty() && claimed < size) {
            AtlasCell cell = frontier.poll();
            state.claim(cell, centre);
            claimed++;
            for (AtlasCell neighbour : generator.lanes(cell)) {
                if (seen.add(neighbour) && canHoldTerritory(neighbour)) {
                    frontier.add(neighbour);
                }
            }
        }
    }

    private AtlasOfficer pickOfficer(AtlasCell centre) {
        Map<String, Integer> visibleTerritories = visibleTerritoryCounts(state, visible);
        Map<String, Float> allowed = openFactions(generator.territoryWeights(centre), context.excludedFactionId(), visibleTerritories);
        if (allowed.isEmpty()) {
            return null;
        }
        List<AtlasOfficer> officers = state.officers();
        Map<String, AtlasOfficer> fled = nearReach(centre) ? firstByFaction(officers, AtlasOfficer::hasFled, allowed) : Map.of();
        String arrivingRankId = lowestWaitingRank(officers, allowed);
        Map<String, AtlasOfficer> arriving = firstByFaction(officers,
                officer -> officer.neverPlaced() && officer.rankId().equals(arrivingRankId), allowed);
        Map<String, Float> weights = new LinkedHashMap<>();
        Map<String, AtlasOfficer> pickable = new HashMap<>();
        allowed.forEach((factionId, regionWeight) -> {
            if (fled.containsKey(factionId)) {
                weights.put(factionId, FLED_WEIGHT);
                pickable.put(factionId, fled.get(factionId));
            } else if (arriving.containsKey(factionId)) {
                weights.put(factionId, absenceWeight(regionWeight, visibleTerritories.getOrDefault(factionId, 0)));
                pickable.put(factionId, arriving.get(factionId));
            }
        });
        String factionId = AtlasHash.race(generator.seed(), OFFICER_SALT, centre.gridX(), centre.gridY(), weights);
        return factionId != null ? pickable.get(factionId) : furthestActiveOfficer(officers, allowed);
    }

    static Map<String, Float> openFactions(Map<String, Float> regionWeights, String excludedFactionId, Map<String, Integer> visibleTerritories) {
        Map<String, Float> open = new LinkedHashMap<>();
        regionWeights.forEach((factionId, weight) -> {
            if (!factionId.equals(excludedFactionId) && visibleTerritories.getOrDefault(factionId, 0) < MAX_VISIBLE_TERRITORIES) {
                open.put(factionId, weight);
            }
        });
        return open;
    }

    static float absenceWeight(float regionWeight, int heldTerritories) {
        return regionWeight * (heldTerritories == 0 ? ABSENT_WEIGHT : 1f / (1 + heldTerritories));
    }

    private static String lowestWaitingRank(List<AtlasOfficer> officers, Map<String, Float> allowed) {
        for (OfficerRank rank : OfficerRanks.all()) {
            for (AtlasOfficer officer : officers) {
                if (officer.neverPlaced() && officer.rankId().equals(rank.id()) && allowed.containsKey(officer.factionId())) {
                    return rank.id();
                }
            }
        }
        return null;
    }

    private static Map<String, AtlasOfficer> firstByFaction(List<AtlasOfficer> officers, Predicate<AtlasOfficer> filter, Map<String, Float> allowed) {
        Map<String, AtlasOfficer> first = new HashMap<>();
        for (AtlasOfficer officer : officers) {
            if (filter.test(officer) && allowed.containsKey(officer.factionId())) {
                first.putIfAbsent(officer.factionId(), officer);
            }
        }
        return first;
    }

    private AtlasOfficer furthestActiveOfficer(List<AtlasOfficer> officers, Map<String, Float> allowed) {
        AtlasOfficer furthest = null;
        double furthestDistance = -1;
        for (AtlasOfficer officer : officers) {
            if (!officer.isActive() || !allowed.containsKey(officer.factionId()) || context.takesPartInOpenRun(state, officer)) {
                continue;
            }
            double distance = distanceToReach(officer.commandCentre());
            if (distance > furthestDistance) {
                furthest = officer;
                furthestDistance = distance;
            }
        }
        return furthest;
    }

    private double distanceToReach(AtlasCell cell) {
        double nearest = Double.MAX_VALUE;
        for (AtlasCell reach : reachable) {
            nearest = Math.min(nearest, Math.hypot(cell.gridX() - (double) reach.gridX(), cell.gridY() - (double) reach.gridY()));
        }
        return nearest;
    }

    private boolean nearReach(AtlasCell centre) {
        return withinLanes(centre, FLED_REACH_LANES).stream().anyMatch(reachable::contains);
    }

    static Map<String, Integer> visibleTerritoryCounts(AtlasState state, Set<AtlasCell> visible) {
        Map<String, Set<AtlasCell>> centresByFaction = new HashMap<>();
        for (AtlasCell cell : visible) {
            AtlasCell centre = state.commandCentreOf(cell);
            if (centre != null) {
                centresByFaction.computeIfAbsent(state.claimedFaction(cell), factionId -> new LinkedHashSet<>()).add(centre);
            }
        }
        Map<String, Integer> counts = new HashMap<>();
        centresByFaction.forEach((factionId, centres) -> counts.put(factionId, centres.size()));
        return counts;
    }
}
