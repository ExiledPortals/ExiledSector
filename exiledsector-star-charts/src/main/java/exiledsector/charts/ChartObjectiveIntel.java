package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.charts.expansion.ChartExpansion;

import java.awt.Color;
import java.util.Set;

public class ChartObjectiveIntel extends BaseIntelPlugin implements ChartRunBound {

    static final String ICON_CATEGORY = "intel";
    static final String ICON_ID = "discovered_entity";
    static final float PARAGRAPH_PAD = 10f;
    static final float BULLET_PAD = 3f;
    private static final Color PROGRESS_COLOUR = new Color(170, 170, 170);

    private final String runId;

    public ChartObjectiveIntel(String runId) {
        this.runId = runId;
    }

    public static ChartObjectiveIntel addFor(SectorAPI sector, ChartRun run) {
        if (run.objective() == null || sector.getIntelManager() == null) {
            return null;
        }
        ChartObjectiveIntel intel = new ChartObjectiveIntel(run.runId());
        intel.setImportant(true);
        sector.getIntelManager().addIntel(intel, true);
        return intel;
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return runId != null && runId.equals(wantedRunId);
    }

    String runId() {
        return runId;
    }

    private ChartRun liveRun() {
        ChartRun run = ChartRuns.currentOrNull();
        return run != null && run.runId().equals(runId) && run.runState() != ChartRunState.CLOSED ? run : null;
    }

    static String objectiveName(ChartObjective objective) {
        return objective == null ? "" : Translation.gameText("chart.objective." + objective.key() + ".name");
    }

    static String descriptionKey(ChartRun run) {
        String objectiveKey = run.objective().key();
        String bossKey = run.objective() == ChartObjective.BOSS ? "." + run.bossType().key() : "";
        return "chart.objective." + objectiveKey + bossKey + ".description";
    }

    static String progressText(ChartRun run) {
        if (run.isObjectiveComplete()) {
            return Translation.gameText("chart.intel.complete");
        }
        int done = run.objectiveTargetsDefeated();
        int total = run.objectiveTargetIds().size();
        return I18n.forGameText(() -> Translation.msg("chart.intel.progress").arg("done", done).arg("total", total).text());
    }

    static String factionName(String factionId) {
        if (factionId == null) {
            return "";
        }
        try {
            FactionAPI faction = Global.getSector().getFaction(factionId);
            return faction == null ? factionId : faction.getDisplayName();
        } catch (RuntimeException e) {
            return factionId;
        }
    }

    static String safe(String text) {
        return text == null ? "" : text.replace("%", "%%");
    }

    @Override
    protected String getName() {
        ChartRun run = liveRun();
        String objective = run == null ? "" : objectiveName(run.objective());
        return I18n.forGameText(() -> Translation.msg("chart.intel.title").arg("objective", objective).text());
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(safe(getName()), getTitleColor(mode), 0f);
        ChartRun run = liveRun();
        if (run != null) {
            info.addPara(safe(progressText(run)), PROGRESS_COLOUR, BULLET_PAD);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        ChartRun run = liveRun();
        if (run == null || run.objective() == null) {
            info.addPara(safe(Translation.gameText("chart.dialog.stale")), 0f);
            return;
        }
        int tier = run.tier();
        String faction = factionName(run.contentFaction());
        info.addPara(safe(Translation.gameText(descriptionKey(run))), 0f);
        info.addPara(safe(I18n.forGameText(() -> Translation.msg("chart.intel.tier").arg("tier", tier).arg("faction", faction).text())),
                PARAGRAPH_PAD);
        info.addPara(safe(progressText(run)), PARAGRAPH_PAD);
        StarSystemAPI system = Global.getSector().getStarSystem(run.systemId());
        for (String expansionId : run.expansionIds()) {
            ChartExpansion expansion = ChartExpansions.byId(expansionId);
            if (expansion == null || !expansion.isActiveIn(system)) {
                continue;
            }
            info.addPara(safe(Translation.gameText("chart.expansion." + expansionId + ".intel")), PARAGRAPH_PAD);
        }
        if (!run.isObjectiveComplete()) {
            info.addPara(safe(Translation.gameText("chart.intel.reward")), PARAGRAPH_PAD);
        }
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
        return null;
    }

    @Override
    public boolean shouldRemoveIntel() {
        return liveRun() == null;
    }
}
