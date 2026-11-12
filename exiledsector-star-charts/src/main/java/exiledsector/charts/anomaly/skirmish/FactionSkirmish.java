package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.ContentPlanSite;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasEntry;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasRunChoice;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.atlas.RedeploymentCatalystUse;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.layout.ChartLayoutRequest;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.turn.ContestOutcomes;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.Translation;
import exiledsector.items.UseVerdict;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.Comparator;
import java.util.List;

public final class FactionSkirmish implements AtlasRule, RunParticipant, ContentPlacer {

    public static final String ANOMALY_ID = "faction_skirmish";
    public static final FactionSkirmish INSTANCE = new FactionSkirmish();
    public static final String NO_SIDE_KEY = "catalyst.refused.skirmish.no_side";
    public static final String UNCLAIMED_KEY = "catalyst.refused.skirmish.unclaimed";
    public static final String COMMAND_CENTRE_KEY = "catalyst.refused.skirmish.command_centre";
    public static final String NO_ATTACKER_KEY = "catalyst.refused.skirmish.no_attacker";

    private FactionSkirmish() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void loadData() {
        SkirmishFactions.load();
    }

    @Override
    public void checkRun(SectorAPI sector, AnomalyRun run) {
        ChartSkirmishes.pushCommander(run.system());
    }

    @Override
    public void fleetGone(AnomalyRun run, CampaignFleetAPI fleet) {
        if (fleet != null && SkirmishSide.ATTACKER.key().equals(fleet.getMemoryWithoutUpdate().getString(ChartIds.SKIRMISH_SIDE_MEMORY_KEY))) {
            ChartSkirmishes.pushCommander(run.system());
        }
    }

    @Override
    public String intelText(AnomalyRun run) {
        return intelLine();
    }

    @Override
    public boolean forcesStationBoss() {
        return true;
    }

    @Override
    public ChartContentPlan adjustPlan(ChartContentPlan plan, ContentPlanSite site) {
        return SkirmishPlanner.apply(plan, site);
    }

    @Override
    public void generate(AnomalyContentSite site) {
        SkirmishBattle.generate(site);
    }

    @Override
    public float atlasWeight(AtlasTraits traits, float weight) {
        return 0f;
    }

    @Override
    public boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return SkirmishContests.at(generator, state, cell) != null;
    }

    @Override
    public UseVerdict insertVerdict(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        String defender = defender(generator, state, cell);
        if (defender == null || generator.node(cell).specialId() != null) {
            return UseVerdict.refused(RedeploymentCatalystUse.SPECIAL_KEY);
        }
        if (!defender.equals(AtlasOpening.territoryOwnerOf(generator, state, cell))) {
            return UseVerdict.refused(RedeploymentCatalystUse.INFESTED_KEY).with("faction", ChartFactions.displayName(defender));
        }
        if (!SkirmishFactions.takesSides(defender)) {
            return UseVerdict.refused(NO_SIDE_KEY).with("faction", ChartFactions.displayName(defender));
        }
        AtlasCell centre = state.commandCentreOf(cell);
        if (centre == null || state.officerAt(centre) == null) {
            return UseVerdict.refused(UNCLAIMED_KEY);
        }
        if (centre.equals(cell)) {
            return UseVerdict.refused(COMMAND_CENTRE_KEY);
        }
        return SkirmishContests.canBeAttacked(generator, state, cell) ? UseVerdict.ALLOW : UseVerdict.refused(NO_ATTACKER_KEY);
    }

    @Override
    public List<AtlasLine> atlasLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        SkirmishContest contest = SkirmishContests.at(generator, state, cell);
        if (contest == null) {
            return List.of();
        }
        return List.of(new AtlasLine(Translation.msg("chart.atlas.node.skirmish")
                .arg("officer", OfficerText.title(contest.attacker()))
                .arg("attacker", ChartFactions.displayName(contest.attacker().factionId()))
                .arg("defender", ChartFactions.displayName(contest.defender().factionId())).text(), AtlasLine.Tone.PLAIN));
    }

    @Override
    public List<String> confirmLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return List.of(Translation.text("chart.atlas.skirmish.final"));
    }

    @Override
    public List<BenchModeSupport.OpenChoice> entryChoices(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        SkirmishContest contest = SkirmishContests.at(generator, state, cell);
        if (contest == null) {
            return List.of();
        }
        String defender = ChartFactions.displayName(contest.defender().factionId());
        String attacker = ChartFactions.displayName(contest.attacker().factionId());
        String officer = OfficerText.title(contest.attacker());
        return List.of(
                new BenchModeSupport.OpenChoice(Translation.msg("chart.atlas.skirmish.join").arg("faction", defender).text(),
                        Translation.msg("chart.atlas.skirmish.defender").arg("defender", defender).arg("officer", officer).text(),
                        new AtlasEntry(cell, SkirmishSide.DEFENDER)),
                new BenchModeSupport.OpenChoice(Translation.msg("chart.atlas.skirmish.join").arg("faction", attacker).text(),
                        Translation.msg("chart.atlas.skirmish.attacker").arg("attacker", attacker).arg("defender", defender).text(),
                        new AtlasEntry(cell, SkirmishSide.ATTACKER)),
                new BenchModeSupport.OpenChoice(Translation.text("chart.atlas.skirmish.stayOut"),
                        Translation.text("chart.atlas.skirmish.neither"), new AtlasEntry(cell, SkirmishSide.NEITHER)));
    }

    @Override
    public AtlasRunNode joinRunNode(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasRunChoice choice) {
        SkirmishContest contest = SkirmishContests.at(generator, state, cell);
        if (contest == null) {
            return node;
        }
        SkirmishSide side = choice instanceof SkirmishSide chosen ? chosen : SkirmishSide.NEITHER;
        AtlasRunNode joined = node.withAnomalyData(ANOMALY_ID, new SkirmishJoin(side, contest.attacker().factionId(), contest.intent()).encode());
        return side == SkirmishSide.DEFENDER ? joined.withAnomalyData(AtlasOfficer.RUN_DATA_KEY, Integer.toString(contest.attacker().recordId())) : joined;
    }

    @Override
    public void runCompleted(SectorAPI sector, ChartRun run) {
        SkirmishJoin join = SkirmishJoin.of(run.atlasNode());
        if (join != null && join.side() == SkirmishSide.ATTACKER && join.contest() != null) {
            ContestOutcomes.settle(AtlasRuntime.generator(sector), AtlasState.get(sector), join.contest(), AtlasCell.unpack(run.atlasNode().cell()), true);
        }
    }

    @Override
    public void runClosed(SectorAPI sector, ChartRun run) {
        SkirmishJoin join = SkirmishJoin.of(run.atlasNode());
        if (join != null && join.side() == SkirmishSide.NEITHER && join.contest() != null) {
            stayedOut(AtlasRuntime.generator(sector), AtlasState.get(sector), AtlasCell.unpack(run.atlasNode().cell()), join.contest());
        }
    }

    static boolean stayedOut(AtlasGenerator generator, AtlasState state, AtlasCell cell, Intent contest) {
        return ContestOutcomes.settle(generator, state, contest, cell, ContestOutcomes.abstractAttackerWins(state, contest, cell));
    }

    public static ChartLayoutRequest joinedOn(ChartLayoutRequest request, SkirmishSide side, AtlasOfficer attacker) {
        SkirmishJoin join = new SkirmishJoin(side, attacker.factionId(), Intent.push(attacker.recordId(), null, Intent.NO_OFFICER));
        return request.withAnomalies(List.of(ANOMALY_ID)).withAnomalyData(ANOMALY_ID, join.encode());
    }

    public static AtlasOfficer anyAttacker(AtlasState state) {
        return state.officers().stream().filter(officer -> officer.isActive() && SkirmishFactions.takesSides(officer.factionId()))
                .min(Comparator.comparingInt(AtlasOfficer::recordId)).orElse(null);
    }

    public static String defender(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasOpening.ownerOf(generator, state, cell);
    }

    public static String attacker(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        SkirmishContest contest = SkirmishContests.at(generator, state, cell);
        return contest == null ? null : contest.attacker().factionId();
    }

    public static boolean isIn(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasAnomalies.effective(generator, state, cell).contains(ANOMALY_ID);
    }
}
