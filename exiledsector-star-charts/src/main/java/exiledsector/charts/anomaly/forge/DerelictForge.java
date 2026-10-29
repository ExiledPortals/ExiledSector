package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.List;
import java.util.Random;

public final class DerelictForge implements ChartAnomaly {

    public static final String ID = "derelict_forge";
    public static final String EXTRA_PROGRAMS_KEY = ID + ".extraPrograms";
    public static final String EXTRA_RUNS_KEY = ID + ".extraRuns";
    public static final String COST_KEY = ID + ".cost";
    public static final String FORGE_TAG = ChartIds.PREFIX + "derelict_forge";
    public static final DerelictForge INSTANCE = new DerelictForge();
    static final String ENTITY_TYPE = ChartIds.PREFIX + "derelict_forge_station";
    static final long PLACEMENT_SALT = 0x464f524745L;

    private DerelictForge() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runId().equals(site.runId())) {
            throw new IllegalStateException("no run to record the derelict forge in");
        }
        place(site, run);
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
        ForgeGuards.arm(forge, site.random(ForgeGuards.GUARD_SALT).nextLong());
        ForgeStates.record(run, programs, ForgeRules.runCount(site.context().anomalyPercent(EXTRA_RUNS_KEY)));
        return forge;
    }

    public static String forgeId(String runId) {
        return ChartIds.PREFIX + "derelict_forge_" + runId;
    }

    public static boolean isForge(SectorEntityToken entity) {
        return entity != null && entity.hasTag(FORGE_TAG);
    }

    static boolean inRunSystem(ChartRun run, LocationAPI location) {
        return run != null && run.runState().isLive() && location != null && run.systemId().equals(ChartIds.locationIdOf(location));
    }

    @Override
    public boolean isActiveIn(LocationAPI location) {
        ChartRun run = ChartRuns.currentOrNull();
        return inRunSystem(run, location) && ForgeStates.runsLeft(run) > 0;
    }

    @Override
    public void loadData() {
        ForgePrograms.load();
    }

    @Override
    public InteractionDialogPlugin dialogFor(SectorEntityToken target) {
        return isForge(target) ? new ForgeDialog() : null;
    }

    @Override
    public String intelText(ChartRun run, LocationAPI location) {
        int runsLeft = ForgeStates.runsLeft(run);
        List<ForgePrograms.Program> programs = ForgeStates.programs(run);
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
