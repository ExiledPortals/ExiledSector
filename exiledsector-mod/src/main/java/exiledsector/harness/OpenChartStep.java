package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.layout.ChartLayoutRequest;
import exiledsector.charts.run.ChartEntryBans;
import exiledsector.charts.run.ChartEntryFuel;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.tree.AtlasTree;
import exiledsector.charts.tree.AtlasTreeData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class OpenChartStep implements HarnessStep {

    static final int ENTRIES_ALLOWED = 1;
    static final float ENTRY_TIMEOUT_SECONDS = 60f;

    record Options(long seed, int tier, List<String> anomalies, String region, String layout, Map<String, String> anomalyData,
                   List<String> atlasNodes) {

        static Options parse(JSONObject step, long defaultSeed) throws JSONException {
            Map<String, String> anomalyData = new LinkedHashMap<>();
            JSONObject dataJson = step.optJSONObject("anomalyData");
            if (dataJson != null) {
                for (Iterator<?> keys = dataJson.keys(); keys.hasNext(); ) {
                    String anomalyId = (String) keys.next();
                    anomalyData.put(anomalyId, dataJson.getString(anomalyId));
                }
            }
            return new Options(step.optLong("seed", defaultSeed), step.optInt("tier", ChartTiers.LOWEST_TIER),
                    HarnessScenario.strings(step.optJSONArray("anomalies")), blankToNull(step.optString("region", null)),
                    blankToNull(step.optString("layout", null)), Collections.unmodifiableMap(anomalyData),
                    HarnessScenario.strings(step.optJSONArray("atlasNodes")));
        }

        private static String blankToNull(String text) {
            return text == null || text.isBlank() ? null : text;
        }

        String unknownIdProblem() {
            if (region != null && !AtlasData.knowsRegion(region)) {
                return "unknown region " + region;
            }
            if (layout != null && !AtlasData.knowsLayout(layout)) {
                return "unknown layout " + layout;
            }
            List<String> unknownNodes = atlasNodes.stream().filter(nodeId -> AtlasTreeData.get().node(nodeId) == null).toList();
            return unknownNodes.isEmpty() ? null : "unknown atlas tree nodes " + unknownNodes;
        }

        ChartLayoutRequest layoutRequest(Random random) {
            ChartGenContext context = ChartGenContext.none().withAnomalies(anomalies);
            if (region != null) {
                context = context.withBiome(region).withStationBossBarred(AtlasData.ambience(region).abyss());
            }
            ChartLayoutRequest request = new ChartLayoutRequest(random.nextLong(), null, tier, context, AtlasData.recipeFor(region, layout), null);
            for (Map.Entry<String, String> entry : anomalyData.entrySet()) {
                request = request.withAnomalyData(entry.getKey(), entry.getValue());
            }
            return request;
        }

        String describe() {
            StringBuilder text = new StringBuilder("seed ").append(seed);
            if (tier != ChartTiers.LOWEST_TIER) {
                text.append(" tier ").append(tier);
            }
            if (!anomalies.isEmpty()) {
                text.append(" ").append(anomalies);
            }
            if (region != null) {
                text.append(" in ").append(region);
            }
            if (layout != null) {
                text.append(" as ").append(layout);
            }
            if (!atlasNodes.isEmpty()) {
                text.append(" with nodes ").append(atlasNodes);
            }
            return text.toString();
        }
    }

    private final Options options;
    private ChartRun run;

    OpenChartStep(Options options) {
        this.options = options;
    }

    @Override
    public String name() {
        return "openChart " + options.describe();
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        SectorAPI sector = Global.getSector();
        if (context.firstFrame()) {
            StepOutcome notInCampaign = HarnessCampaign.notInCampaign("openChart");
            return notInCampaign != null ? notInCampaign : openAndEnter(sector);
        }
        HarnessCampaign.unpause(sector);
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        if (playerFleet.isInHyperspaceTransition()) {
            return new StepOutcome(false, false, "the player fleet is still jumping into run " + run.runId());
        }
        if (system == null || playerFleet.getContainingLocation() != system) {
            return new StepOutcome(false, false, "the player fleet is in " + playerFleet.getContainingLocation().getName() + ", not run " + run.runId());
        }
        context.results().addData("chartRun", runData(run, system));
        List<String> dropped = options.anomalies().stream().filter(anomalyId -> !run.anomalyIds().contains(anomalyId)).toList();
        if (!dropped.isEmpty()) {
            return StepOutcome.failed("run " + run.runId() + " dropped the forced anomalies " + dropped + "; it has " + run.anomalyIds());
        }
        return StepOutcome.passed("entered Star Chart run " + run.runId() + " in " + system.getName() + " (tier " + run.tier()
                + (run.hasBoss() ? ", with a boss" : ", with no boss") + ")");
    }

    private StepOutcome openAndEnter(SectorAPI sector) {
        String unknownId = options.unknownIdProblem();
        if (unknownId != null) {
            return StepOutcome.failed(unknownId);
        }
        Random random = new Random(options.seed());
        ChartRunActions.OpenResult opened = ChartRunActions.openBesidePlayer(sector, ENTRIES_ALLOWED, random, options.layoutRequest(random),
                AtlasTree.chartLinesOf(options.atlasNodes()));
        if (opened != ChartRunActions.OpenResult.OPENED) {
            return StepOutcome.failed("could not open a Star Chart run: " + opened);
        }
        run = ChartRuns.live();
        SectorEntityToken outerWormhole = sector.getEntityById(run.outerWormholeId());
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (outerWormhole == null) {
            return StepOutcome.failed("run " + run.runId() + " opened without an outer wormhole");
        }
        if (!ChartRunActions.enter(sector, run, outerWormhole)) {
            return StepOutcome.failed("could not enter run " + run.runId() + ": banned hull sizes in the fleet " + ChartEntryBans.breached(run, playerFleet)
                    + ", fuel " + (ChartEntryFuel.canAfford(run, playerFleet) ? "enough" : "short of " + ChartEntryFuel.cost(run)));
        }
        return StepOutcome.RUNNING;
    }

    private static JSONObject runData(ChartRun run, StarSystemAPI system) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("runId", run.runId());
        data.put("system", system.getName());
        data.put("tier", run.tier());
        data.put("hasBoss", run.hasBoss());
        data.put("anomalies", new JSONArray(run.anomalyIds()));
        data.put("planets", system.getPlanets().stream().filter(planet -> !planet.isStar()).count());
        return data;
    }

    @Override
    public float timeoutSeconds() {
        return ENTRY_TIMEOUT_SECONDS;
    }
}
