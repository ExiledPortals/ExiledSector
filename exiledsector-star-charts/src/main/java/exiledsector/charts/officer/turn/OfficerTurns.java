package exiledsector.charts.officer.turn;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.CommissionHandover;
import exiledsector.charts.atlas.CommissionHandover.Handover;
import exiledsector.charts.atlas.TerritoryContext;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OfficerTurns {

    static final String VICTORY_STATE_KEY = "officer_victory";
    private static final Logger LOG = Logger.getLogger(OfficerTurns.class);
    private static final List<OfficerTurnListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile IntentResolvers resolvers = IntentResolvers.ABSTRACT;

    private OfficerTurns() {
    }

    public static TurnResult bossKilled(AtlasGenerator generator, AtlasState state, ChartRun run) {
        return bossKilled(generator, state, run == null ? null : OfficerFleets.recordIdOf(run.atlasNode()), TerritoryContext.current());
    }

    static TurnResult bossKilled(AtlasGenerator generator, AtlasState state, Integer officerRecordId, TerritoryContext context) {
        if (officerRecordId != null && state.officer(officerRecordId) != null) {
            return take(generator, state, TurnCause.officerDefeated(officerRecordId), context);
        }
        if (state.addOtherBossKill() < OfficerTurnTables.rules().otherBossKillsPerTurn()) {
            return null;
        }
        state.clearOtherBossKills();
        return take(generator, state, TurnCause.bossKills(), context);
    }

    public static void battleOccurred(BattleAPI battle, CampaignFleetAPI primaryWinner) {
        if (battle == null || primaryWinner == null || !battle.isPlayerInvolved() || battle.onPlayerSide(primaryWinner)) {
            return;
        }
        ChartRun run = ChartRuns.live();
        if (run == null || run.anomalyState(VICTORY_STATE_KEY) != null) {
            return;
        }
        Integer officerRecordId = officerIn(battle.getNonPlayerSideSnapshot());
        if (officerRecordId == null) {
            return;
        }
        SectorAPI sector = Global.getSector();
        AtlasState state = AtlasState.getIfCreated(sector);
        if (state == null || state.officer(officerRecordId) == null) {
            return;
        }
        run.recordAnomalyState(VICTORY_STATE_KEY, Integer.toString(officerRecordId));
        try {
            take(AtlasRuntime.generator(sector), state, TurnCause.officerVictorious(officerRecordId), TerritoryContext.current());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Taking the Atlas officer turn after a lost battle failed", e);
        }
    }

    private static Integer officerIn(List<CampaignFleetAPI> fleets) {
        if (fleets == null) {
            return null;
        }
        for (CampaignFleetAPI fleet : fleets) {
            Object recordId = fleet.getMemoryWithoutUpdate().get(OfficerFleets.RECORD_MEMORY_KEY);
            if (recordId instanceof Number number) {
                return number.intValue();
            }
        }
        return null;
    }

    public static TurnResult take(AtlasGenerator generator, AtlasState state, TurnCause cause, TerritoryContext context) {
        List<Handover> handovers = CommissionHandover.isNeeded(state, context.excludedFactionId())
                ? CommissionHandover.handOver(generator, state, AtlasReach.visible(generator, state), context)
                : List.of();
        AtlasCell openRunCell = context.openRunCell();
        TurnSetup setup = new TurnSetup(OfficerTurnTables.rules(), OfficerRanks.all(), resolvers, openRunCell, openRunOfficerId(state, cause,
                openRunCell), context.catalogue(), context.startingItems());
        TurnResult result = OfficerTurn.take(TurnTerrain.of(generator, state, openRunCell), state, cause, setup);
        if (!handovers.isEmpty()) {
            result = result.withHandovers(handovers);
            state.setLastTurnResult(result.encode());
        }
        for (OfficerTurnListener listener : LISTENERS) {
            try {
                listener.turnTaken(generator, state, result, openRunCell);
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] An Atlas officer turn listener failed", e);
            }
        }
        return result;
    }

    public static void handOverCommission(AtlasGenerator generator, AtlasState state, Set<AtlasCell> visible, TerritoryContext context) {
        List<Handover> handovers = CommissionHandover.handOver(generator, state, visible, context);
        if (handovers.isEmpty()) {
            return;
        }
        state.setLastTurnResult(lastTurn(state).withHandovers(handovers).encode());
        state.markOfficerTurnUnseen();
    }

    static int openRunOfficerId(AtlasState state, TurnCause cause, AtlasCell openRunCell) {
        if (cause.kind() == TurnCause.Kind.OFFICER_DEFEATED || cause.kind() == TurnCause.Kind.OFFICER_VICTORIOUS) {
            return cause.officerRecordId();
        }
        AtlasOfficer holder = openRunCell == null ? null : state.officerAt(openRunCell);
        return holder == null ? Intent.NO_OFFICER : holder.recordId();
    }

    public static List<Intent> declaredIntents(AtlasState state) {
        return OfficerTurn.decodeIntents(state.declaredIntents());
    }

    public static TurnResult lastTurn(AtlasState state) {
        return TurnResult.decode(state.lastTurnResult());
    }

    public static boolean hasUnseenTurn(AtlasState state) {
        return state.officerTurn() > state.seenOfficerTurn();
    }

    public static AtlasOfficer fallenOfficer(AtlasState state, int recordId) {
        return lastTurn(state).fallen().get(recordId);
    }

    public static IntentResolvers resolvers() {
        return resolvers;
    }

    public static void useResolvers(IntentResolvers intentResolvers) {
        resolvers = intentResolvers == null ? IntentResolvers.ABSTRACT : intentResolvers;
    }

    public static void addListener(OfficerTurnListener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(OfficerTurnListener listener) {
        LISTENERS.remove(listener);
    }
}
