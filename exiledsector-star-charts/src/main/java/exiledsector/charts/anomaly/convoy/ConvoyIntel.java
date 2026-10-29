package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.ui.VanillaText;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConvoyIntel extends BaseIntelPlugin implements ChartRunBound {

    static final String ICON_CATEGORY = "intel";
    static final String ICON_ID = "discovered_entity";
    static final float PARAGRAPH_PAD = 10f;
    static final float BULLET_PAD = 3f;
    private static final Color STATUS_COLOUR = new Color(170, 170, 170);

    private final String runId;
    private final int convoyIndex;
    private transient ConvoyScript script;

    public ConvoyIntel(String runId, int convoyIndex) {
        this.runId = runId;
        this.convoyIndex = convoyIndex;
    }

    static ConvoyIntel addFor(SectorAPI sector, ConvoyScript script, ConvoyState convoy) {
        if (sector.getIntelManager() == null) {
            return null;
        }
        ConvoyIntel intel = new ConvoyIntel(script.runId(), convoy.index());
        intel.script = script;
        sector.getIntelManager().addIntel(intel, true);
        return intel;
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return runId != null && runId.equals(wantedRunId);
    }

    private ConvoyState convoy() {
        if (script == null || script.isDone()) {
            script = ConvoyScript.in(Global.getSector().getStarSystem(ChartIds.systemId(runId)));
        }
        return script == null || !script.belongsToRun(runId) ? null : script.convoy(convoyIndex);
    }

    private String destinationName(ConvoyState convoy) {
        SectorEntityToken destination = script.entity(convoy.destinationId());
        return destination == null ? "" : destination.getName();
    }

    @Override
    protected String getName() {
        ConvoyState convoy = convoy();
        String destination = convoy == null ? "" : destinationName(convoy);
        return I18n.forGameText(() -> Translation.msg("chart.convoy.intel.title").arg("destination", destination).text());
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(VanillaText.escapeFormat(getName()), getTitleColor(mode), 0f);
        ConvoyState convoy = convoy();
        if (convoy != null) {
            info.addPara(VanillaText.escapeFormat(statusText(convoy)), STATUS_COLOUR, BULLET_PAD);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        ConvoyState convoy = convoy();
        if (convoy == null) {
            VanillaText.plainPara(info, Translation.gameText("chart.dialog.stale"), 0f, Misc.getTextColor());
            return;
        }
        String destination = destinationName(convoy);
        Color text = Misc.getTextColor();
        VanillaText.plainPara(info, I18n.forGameText(() -> Translation.msg("chart.convoy.intel.description").arg("destination", destination).text()),
                0f, text);
        List<StyledText> stops = stopNames(convoy);
        VanillaText.plainPara(info, I18n.forGameText(() -> stops.isEmpty()
                ? Translation.msg("chart.convoy.intel.routeDirect").arg("destination", destination).text()
                : Translation.msg("chart.convoy.intel.route").arg("stops", Translation.list(stops)).arg("destination", destination).text()),
                PARAGRAPH_PAD, text);
        VanillaText.plainPara(info, statusText(convoy), PARAGRAPH_PAD, text);
        if (convoy.isHostile()) {
            VanillaText.addPara(info, I18n.forGameText(() -> Translation.styled("chart.convoy.intel.hostile")), PARAGRAPH_PAD, text);
        }
        int tier = script.tier();
        VanillaText.addPara(info, I18n.forGameText(() -> Translation.msg("chart.convoy.intel.manifest").arg("cargo", ConvoyCargo.manifest(tier))
                .styled()), PARAGRAPH_PAD, text);
    }

    private List<StyledText> stopNames(ConvoyState convoy) {
        List<StyledText> names = new ArrayList<>();
        for (String waypointId : convoy.waypointIds()) {
            SectorEntityToken waypoint = script.entity(waypointId);
            if (waypoint != null) {
                names.add(StyledText.of(waypoint.getName()));
            }
        }
        return names;
    }

    String statusText(ConvoyState convoy) {
        String destination = destinationName(convoy);
        CampaignClockAPI clock = Global.getSector().getClock();
        return I18n.forGameText(() -> switch (convoy.phase()) {
            case WAITING -> script.playerHasArrived()
                    ? Translation.msg("chart.convoy.intel.status.departs").count(days(script.daysUntilDeparture(clock))).text()
                    : Translation.msg("chart.convoy.intel.status.departsAfterArrival").count(days(ConvoyRules.DEPARTURE_DAYS)).text();
            case EN_ROUTE -> Translation.msg("chart.convoy.intel.status.enRoute").arg("destination", destination)
                    .count(days(daysToDestination(convoy, clock))).text();
            case UNLOADING -> Translation.msg("chart.convoy.intel.status.unloading").arg("destination", destination).text();
            case DELIVERED -> convoy.hasCache() ? Translation.msg("chart.convoy.intel.status.delivered").arg("destination", destination).text()
                    : Translation.gameText(convoy.isCacheTaken() ? "chart.convoy.intel.status.raided" : "chart.convoy.intel.status.empty");
            case LOST -> Translation.text("chart.convoy.intel.status.lost");
        });
    }

    static int days(float days) {
        return Math.max(1, Math.round(days));
    }

    float daysToDestination(ConvoyState convoy, CampaignClockAPI clock) {
        CampaignFleetAPI freighters = script.fleet(convoy, ConvoyRole.FREIGHTERS);
        SectorEntityToken destination = script.entity(convoy.destinationId());
        if (freighters == null || destination == null) {
            return 0f;
        }
        List<Vector2f> stops = new ArrayList<>();
        List<String> waypointIds = convoy.waypointIds();
        int next = nextWaypoint(freighters, waypointIds);
        for (int i = next; i < waypointIds.size(); i++) {
            SectorEntityToken waypoint = script.entity(waypointIds.get(i));
            if (waypoint != null) {
                stops.add(waypoint.getLocation());
            }
        }
        int stopsLeft = stops.size();
        stops.add(destination.getLocation());
        float speed = Math.max(1f, freighters.getTravelSpeed());
        return ConvoyRoute.pathLength(freighters.getLocation(), stops) / speed / clock.getSecondsPerDay() + stopsLeft * ConvoyRules.STOP_DAYS;
    }

    private int nextWaypoint(CampaignFleetAPI freighters, List<String> waypointIds) {
        FleetAssignmentDataAPI current = freighters.getCurrentAssignment();
        SectorEntityToken target = current == null ? null : current.getTarget();
        int index = target == null ? -1 : waypointIds.indexOf(target.getId());
        return index < 0 ? waypointIds.size() : index;
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName(ICON_CATEGORY, ICON_ID);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_EXPLORATION);
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        ConvoyState convoy = convoy();
        if (convoy == null || convoy.phase() == ConvoyState.Phase.LOST) {
            return null;
        }
        CampaignFleetAPI freighters = convoy.phase() == ConvoyState.Phase.DELIVERED ? null : script.fleet(convoy, ConvoyRole.FREIGHTERS);
        return freighters != null ? freighters : script.entity(convoy.destinationId());
    }

    @Override
    public boolean shouldRemoveIntel() {
        return convoy() == null;
    }
}
