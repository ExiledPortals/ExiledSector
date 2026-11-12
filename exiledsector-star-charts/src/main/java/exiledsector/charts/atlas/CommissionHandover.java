package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRanks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CommissionHandover {

    private static final long TERRITORY_SALT = 0x434f4d5445525249L;
    private static final long WAITING_SALT = 0x434f4d57414954L;

    public record Handover(AtlasOfficer predecessor, AtlasOfficer successor) {
    }

    private final AtlasGenerator generator;
    private final AtlasState state;
    private final Set<AtlasCell> visible;
    private final TerritoryContext context;
    private final String commissionFactionId;

    private CommissionHandover(AtlasGenerator generator, AtlasState state, Set<AtlasCell> visible, TerritoryContext context) {
        this.generator = generator;
        this.state = state;
        this.visible = visible;
        this.context = context;
        this.commissionFactionId = context.excludedFactionId();
    }

    public static boolean isNeeded(AtlasState state, String commissionFactionId) {
        return commissionFactionId != null && state.officers().stream().anyMatch(officer -> leavesTheBoard(officer, commissionFactionId));
    }

    public static List<Handover> handOver(AtlasGenerator generator, AtlasState state, Set<AtlasCell> visible, TerritoryContext context) {
        if (!isNeeded(state, context.excludedFactionId())) {
            return List.of();
        }
        return new CommissionHandover(generator, state, visible, context).handOverAll();
    }

    private static boolean leavesTheBoard(AtlasOfficer officer, String commissionFactionId) {
        return officer.factionId().equals(commissionFactionId) && (officer.isActive() || officer.hasFled());
    }

    private List<Handover> handOverAll() {
        List<Handover> handovers = new ArrayList<>();
        for (AtlasOfficer officer : state.officers()) {
            if (officer.factionId().equals(commissionFactionId) && officer.hasFled()) {
                addIfReplaced(handovers, replaceWaiting(officer));
            }
        }
        for (AtlasOfficer officer : state.officers()) {
            if (officer.factionId().equals(commissionFactionId) && officer.isActive() && !holdsOpenRun(officer)) {
                addIfReplaced(handovers, handOverTerritory(officer));
            }
        }
        return handovers;
    }

    private boolean holdsOpenRun(AtlasOfficer officer) {
        AtlasCell openRunCell = context.openRunCell();
        return openRunCell != null && (officer.commandCentre().equals(openRunCell) || officer.commandCentre().equals(state.commandCentreOf(openRunCell)));
    }

    private static void addIfReplaced(List<Handover> handovers, Handover handover) {
        if (handover != null) {
            handovers.add(handover);
        }
    }

    private Handover handOverTerritory(AtlasOfficer predecessor) {
        AtlasCell centre = predecessor.commandCentre();
        String factionId = territoryFaction(centre);
        if (factionId == null) {
            state.breakUp(centre);
            return replaceWaiting(predecessor);
        }
        state.removeOfficer(predecessor.recordId());
        AtlasOfficer successor = newOfficer(factionId, predecessor)
                .takingOverFrom(predecessor, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, centre));
        successor = state.withLeftBehindItems(successor);
        state.putOfficer(successor);
        return new Handover(predecessor, successor);
    }

    private Handover replaceWaiting(AtlasOfficer predecessor) {
        String factionId = waitingFaction(predecessor);
        if (factionId == null) {
            return null;
        }
        state.removeOfficer(predecessor.recordId());
        state.leaveBehind(predecessor.carried());
        AtlasOfficer successor = newOfficer(factionId, predecessor).waitingInPlaceOf(predecessor);
        state.putOfficer(successor);
        return new Handover(predecessor, successor);
    }

    private AtlasOfficer newOfficer(String factionId, AtlasOfficer predecessor) {
        return OfficerBoardGenerator.atRank(state.seed(), state.newOfficerRecordId(), factionId, predecessor.rankId(), OfficerRanks.all(),
                context.catalogue());
    }

    private String territoryFaction(AtlasCell centre) {
        Map<String, Integer> visibleTerritories = AtlasTerritoryPlacer.visibleTerritoryCounts(state, visible);
        Map<String, Float> regionWeights = generator.territoryWeights(centre);
        Map<String, Float> open = AtlasTerritoryPlacer.openFactions(regionWeights, commissionFactionId, visibleTerritories);
        if (open.isEmpty()) {
            open = withoutCommission(regionWeights);
        }
        Map<String, Float> weights = new LinkedHashMap<>();
        open.forEach((factionId, regionWeight) ->
                weights.put(factionId, AtlasTerritoryPlacer.absenceWeight(regionWeight, visibleTerritories.getOrDefault(factionId, 0))));
        return AtlasHash.race(generator.seed(), TERRITORY_SALT, centre.gridX(), centre.gridY(), weights);
    }

    private String waitingFaction(AtlasOfficer predecessor) {
        Map<String, Float> boardFactions = withoutCommission(TerritoryFactions.boardFactions(generator.rules()));
        return AtlasHash.race(generator.seed(), WAITING_SALT, predecessor.recordId(), OfficerRanks.order(predecessor.rankId()), boardFactions);
    }

    private Map<String, Float> withoutCommission(Map<String, Float> weights) {
        Map<String, Float> kept = new LinkedHashMap<>(weights);
        kept.remove(commissionFactionId);
        return kept;
    }
}
