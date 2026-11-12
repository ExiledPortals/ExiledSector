package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public final class AtlasTerritoryTransfer {

    public enum Result {
        JOINED,
        RELEASED,
        COMMAND_CENTRE
    }

    private AtlasTerritoryTransfer() {
    }

    public static Result transfer(AtlasGenerator generator, AtlasState state, AtlasCell cell, String factionId) {
        if (state.officerAt(cell) != null) {
            return Result.COMMAND_CENTRE;
        }
        AtlasCell formerCentre = state.commandCentreOf(cell);
        AtlasCell centre = neighbouringCentres(generator, state, cell).get(factionId);
        if (centre == null) {
            state.release(cell);
        } else {
            state.claim(cell, centre);
        }
        releaseCutOff(generator::lanes, state, formerCentre);
        return centre == null ? Result.RELEASED : Result.JOINED;
    }

    public static boolean transferTo(Function<AtlasCell, ? extends Collection<AtlasCell>> lanes, AtlasState state, AtlasCell cell,
                                     AtlasCell commandCentre) {
        if (state.officerAt(cell) != null || state.officerAt(commandCentre) == null) {
            return false;
        }
        AtlasCell formerCentre = state.commandCentreOf(cell);
        state.claim(cell, commandCentre);
        releaseCutOff(lanes, state, formerCentre);
        return true;
    }

    public static void vacate(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        if (state.officerAt(cell) != null) {
            dismissOfficer(state, cell);
            return;
        }
        AtlasCell formerCentre = state.commandCentreOf(cell);
        state.release(cell);
        releaseCutOff(generator::lanes, state, formerCentre);
    }

    public static void dismissOfficer(AtlasState state, AtlasCell commandCentre) {
        AtlasOfficer officer = state.officerAt(commandCentre);
        if (officer != null) {
            state.breakUp(commandCentre);
            state.putOfficer(officer.losingCommand(AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.FLED_HISTORY, commandCentre)));
        }
    }

    public static Map<String, AtlasCell> neighbouringCentres(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        TreeSet<AtlasCell> neighbours = new TreeSet<>(Comparator.comparingLong(AtlasCell::pack));
        neighbours.addAll(generator.lanes(cell));
        AtlasCell ownCentre = state.commandCentreOf(cell);
        Map<String, AtlasCell> centres = new LinkedHashMap<>();
        for (AtlasCell neighbour : neighbours) {
            AtlasCell centre = state.commandCentreOf(neighbour);
            if (centre != null && !centre.equals(ownCentre)) {
                centres.putIfAbsent(state.claimedFaction(neighbour), centre);
            }
        }
        return centres;
    }

    private static void releaseCutOff(Function<AtlasCell, ? extends Collection<AtlasCell>> lanes, AtlasState state, AtlasCell commandCentre) {
        if (commandCentre == null || state.officerAt(commandCentre) == null) {
            return;
        }
        Set<AtlasCell> connected = new HashSet<>(Set.of(commandCentre));
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>(connected);
        while (!frontier.isEmpty()) {
            for (AtlasCell neighbour : lanes.apply(frontier.poll())) {
                if (commandCentre.equals(state.commandCentreOf(neighbour)) && connected.add(neighbour)) {
                    frontier.add(neighbour);
                }
            }
        }
        for (AtlasCell claimed : state.claimsOf(commandCentre)) {
            if (!connected.contains(claimed)) {
                state.release(claimed);
            }
        }
    }
}
