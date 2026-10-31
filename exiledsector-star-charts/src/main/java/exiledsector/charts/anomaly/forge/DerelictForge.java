package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.run.ChartRun;

import java.util.List;
import java.util.Random;

public final class DerelictForge implements ContentPlacer, RunParticipant {

    public static final String ANOMALY_ID = "derelict_forge";
    public static final String EXTRA_PROGRAMS_KEY = ChartEffect.CHART_FORGE_PROGRAMS.anomalyTuningKey();
    public static final String EXTRA_RUNS_KEY = ChartEffect.CHART_FORGE_RUNS.anomalyTuningKey();
    public static final String COST_KEY = ChartEffect.CHART_FORGE_COST_PERCENT.anomalyTuningKey();
    public static final String FORGE_TAG = ChartIds.PREFIX + "derelict_forge";
    public static final DerelictForge INSTANCE = new DerelictForge();
    static final String ENTITY_TYPE = ChartIds.PREFIX + "derelict_forge_station";
    static final long PLACEMENT_SALT = 0x464f524745L;

    private DerelictForge() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        place(site, site.run());
    }

    static SectorEntityToken place(AnomalyContentSite site, ChartRun run) {
        StarSystemAPI system = site.system();
        SectorEntityToken centre = system.getCenter() != null ? system.getCenter() : system.getStar();
        Random random = site.random(PLACEMENT_SALT);
        ChartSites.Orbit orbit = null;
        for (int band = 0; orbit == null && centre != null && band < ForgeRules.ORBIT_BANDS; band++) {
            float bandMin = ForgeRules.ORBIT_MIN + band * ForgeRules.ORBIT_BAND_WIDTH;
            orbit = site.clearOrbit(centre, bandMin, bandMin + ForgeRules.ORBIT_BAND_WIDTH, random);
        }
        if (orbit == null) {
            throw new IllegalStateException("no clear orbit for the derelict forge");
        }
        List<ForgePrograms.Program> programs = ForgePrograms.roll(site.tier(),
                ForgeRules.programCount(site.context().anomalyPercent(EXTRA_PROGRAMS_KEY)), random);
        SectorEntityToken forge = site.addEntity(forgeId(site.runId()), Translation.gameText("chart.forge.name"), ENTITY_TYPE, Factions.NEUTRAL, orbit);
        forge.addTag(FORGE_TAG);
        VanillaDefenders.arm(forge, site.random(ForgeGuards.GUARD_SALT).nextLong());
        ForgeStates.record(run, programs, ForgeRules.runCount(site.context().anomalyPercent(EXTRA_RUNS_KEY)));
        return forge;
    }

    public static String forgeId(String runId) {
        return ChartIds.PREFIX + "derelict_forge_" + runId;
    }

    public static boolean isForge(SectorEntityToken entity) {
        return entity != null && entity.hasTag(FORGE_TAG);
    }

    @Override
    public void loadData() {
        ForgePrograms.load();
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return isForge(target) ? new ForgeDialog() : null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        int runsLeft = ForgeStates.runsLeft(run.run());
        List<ForgePrograms.Program> programs = ForgeStates.programs(run.run());
        if (runsLeft <= 0 || programs.isEmpty()) {
            return null;
        }
        return I18n.forGameText(() -> {
            List<StyledText> names = programs.stream().map(program -> StyledText.styled(ForgeTexts.programName(program), Style.HIGHLIGHT)).toList();
            return Translation.msg("chart.anomaly.derelict_forge.intel").count(runsLeft).arg("count", runsLeft).arg("programs", Translation.list(names))
                    .text();
        });
    }
}
