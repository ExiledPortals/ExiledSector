package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.ContentPlanSite;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasRunChoice;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.turn.ContestOutcomes;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.Translation;

import java.util.List;

public final class FactionSkirmish implements AtlasRule, RunParticipant, ContentPlacer {

    public static final String ANOMALY_ID = "faction_skirmish";
    public static final FactionSkirmish INSTANCE = new FactionSkirmish();

    private FactionSkirmish() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void fleetGone(AnomalyRun run, CampaignFleetAPI fleet) {
        if (FightingSide.of(fleet) == FightingSide.DEFENDER) {
            SkirmishAdvance.pushAttacker(run.system());
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
        return generator.node(cell).specialId() == null && SkirmishContests.at(generator, state, cell) != null;
    }

    @Override
    public List<AtlasLine> atlasLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        SkirmishContest contest = SkirmishContests.at(generator, state, cell);
        if (contest == null) {
            return List.of();
        }
        return List.of(new AtlasLine(Translation.msg("chart.atlas.node.skirmish")
                .arg("attacker", OfficerText.title(contest.attacker()))
                .arg("attackerFaction", ChartFactions.displayName(contest.attacker().factionId()))
                .arg("defender", OfficerText.title(contest.defender()))
                .arg("defenderFaction", ChartFactions.displayName(contest.defender().factionId())).text(), AtlasLine.Tone.PLAIN));
    }

    @Override
    public List<String> confirmLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return List.of(Translation.text("chart.atlas.skirmish.final"));
    }

    @Override
    public AtlasRunNode joinRunNode(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasRunChoice choice) {
        SkirmishContest contest = SkirmishContests.at(generator, state, cell);
        if (contest == null) {
            return node;
        }
        AtlasOfficer attacker = contest.attacker();
        AtlasOfficer defender = contest.defender();
        SkirmishFight fight = new SkirmishFight(attacker.factionId(), attacker.rankId(), defender.rankId(), contest.intent());
        return node.withAnomalyData(ANOMALY_ID, fight.encode())
                .withAnomalyData(AtlasOfficer.FIGHTING_DATA_KEY, OfficerFleets.fightingRunData(attacker.recordId(), defender.recordId()));
    }

    @Override
    public void runCompleted(SectorAPI sector, ChartRun run) {
        settle(sector, run);
    }

    @Override
    public void runClosed(SectorAPI sector, ChartRun run) {
        if (!run.isObjectiveComplete()) {
            settle(sector, run);
        }
    }

    private static void settle(SectorAPI sector, ChartRun run) {
        SkirmishFight fight = SkirmishFight.of(run.atlasNode());
        SkirmishFlagships flagships = SkirmishFlagships.of(run);
        if (fight != null && fight.contest() != null && flagships != null) {
            settle(AtlasRuntime.generator(sector), AtlasState.get(sector), AtlasCell.unpack(run.atlasNode().cell()), fight.contest(), flagships.result(),
                    flagships.loserBeatenByPlayer());
        }
    }

    static boolean settle(AtlasGenerator generator, AtlasState state, AtlasCell cell, Intent contest, SkirmishResult result) {
        return settle(generator, state, cell, contest, result, false);
    }

    static boolean settle(AtlasGenerator generator, AtlasState state, AtlasCell cell, Intent contest, SkirmishResult result, boolean loserBeatenByPlayer) {
        return switch (result) {
            case ATTACKER_WINS -> ContestOutcomes.settle(generator, state, contest, cell, true, loserBeatenByPlayer);
            case DEFENDER_WINS -> ContestOutcomes.settle(generator, state, contest, cell, false, loserBeatenByPlayer);
            case DROPPED -> {
                ContestOutcomes.drop(state, contest);
                yield false;
            }
            case UNDECIDED -> false;
        };
    }
}
