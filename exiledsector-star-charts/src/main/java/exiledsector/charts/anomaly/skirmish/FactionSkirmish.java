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
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasEntry;
import exiledsector.charts.atlas.AtlasRunChoice;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.atlas.RedeploymentCatalystUse;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.layout.ChartLayoutRequest;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.Translation;
import exiledsector.items.UseVerdict;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.List;

public final class FactionSkirmish implements AtlasRule, RunParticipant, ContentPlacer {

    public static final String ANOMALY_ID = "faction_skirmish";
    public static final FactionSkirmish INSTANCE = new FactionSkirmish();
    public static final String NO_SIDE_KEY = "catalyst.refused.skirmish.no_side";
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
    public boolean fitsAt(AtlasTraits traits) {
        if (traits.specialId() != null || traits.infestationId() != null || !SkirmishFactions.takesSides(traits.factionId())) {
            return false;
        }
        return traits.neighbourFactionIds().stream().anyMatch(neighbour -> !neighbour.equals(traits.factionId()) && SkirmishFactions.takesSides(neighbour));
    }

    @Override
    public boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return SkirmishFactions.takesSides(defender(generator, state, cell)) && attacker(generator, state, cell) != null;
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
        return attacker(generator, state, cell) == null ? UseVerdict.refused(NO_ATTACKER_KEY) : UseVerdict.ALLOW;
    }

    @Override
    public List<AtlasLine> atlasLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return List.of(new AtlasLine(Translation.msg("chart.atlas.node.skirmish")
                .arg("attacker", ChartFactions.displayName(attacker(generator, state, cell)))
                .arg("defender", ChartFactions.displayName(defender(generator, state, cell))).text(), AtlasLine.Tone.PLAIN));
    }

    @Override
    public List<String> confirmLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return List.of(Translation.text("chart.atlas.skirmish.final"));
    }

    @Override
    public List<BenchModeSupport.OpenChoice> entryChoices(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        String defender = ChartFactions.displayName(defender(generator, state, cell));
        String attacker = ChartFactions.displayName(attacker(generator, state, cell));
        return List.of(
                new BenchModeSupport.OpenChoice(Translation.msg("chart.atlas.skirmish.join").arg("faction", defender).text(),
                        Translation.msg("chart.atlas.skirmish.defender").arg("defender", defender).text(), new AtlasEntry(cell, SkirmishSide.DEFENDER)),
                new BenchModeSupport.OpenChoice(Translation.msg("chart.atlas.skirmish.join").arg("faction", attacker).text(),
                        Translation.msg("chart.atlas.skirmish.attacker").arg("attacker", attacker).arg("defender", defender).text(),
                        new AtlasEntry(cell, SkirmishSide.ATTACKER)),
                new BenchModeSupport.OpenChoice(Translation.text("chart.atlas.skirmish.stayOut"),
                        Translation.msg("chart.atlas.skirmish.neither").arg("defender", defender).text(), new AtlasEntry(cell, SkirmishSide.NEITHER)));
    }

    @Override
    public AtlasRunNode joinRunNode(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasRunChoice choice) {
        String attackerId = attacker(generator, state, cell);
        if (attackerId == null) {
            return node;
        }
        SkirmishSide side = choice instanceof SkirmishSide chosen ? chosen : SkirmishSide.NEITHER;
        return node.withAnomalyData(ANOMALY_ID, new SkirmishJoin(side, attackerId).encode());
    }

    @Override
    public void runCompleted(SectorAPI sector, ChartRun run) {
        SkirmishJoin join = SkirmishJoin.of(run.atlasNode());
        if (join != null && join.attackerId() != null && sector.getFaction(join.attackerId()) != null) {
            captureForWinner(AtlasRuntime.generator(sector), AtlasState.get(sector), AtlasCell.unpack(run.atlasNode().cell()), join);
        }
    }

    static void captureForWinner(AtlasGenerator generator, AtlasState state, AtlasCell cell, SkirmishJoin join) {
        if (join.side() == SkirmishSide.ATTACKER && join.attackerId() != null) {
            state.capture(generator, cell, join.attackerId());
        }
    }

    public static ChartLayoutRequest joinedOn(ChartLayoutRequest request, SkirmishSide side) {
        return request.withAnomalies(List.of(ANOMALY_ID)).withAnomalyData(ANOMALY_ID, new SkirmishJoin(side, null).encode());
    }

    public static String defender(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasOpening.ownerOf(generator, state, cell);
    }

    public static String attacker(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasBorders.attacker(generator, state, cell, SkirmishFactions::takesSides);
    }

    public static boolean isIn(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasAnomalies.effective(generator, state, cell).contains(ANOMALY_ID);
    }
}
