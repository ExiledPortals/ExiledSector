package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.GuardedStation;
import exiledsector.charts.anomaly.OrbitSearch;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.crafting.ForgeOperation;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

public final class DerelictForge implements ContentPlacer, RunParticipant {

    public static final String ANOMALY_ID = "derelict_forge";
    public static final String EXTRA_RUNS_KEY = ChartModifier.CHART_FORGE_RUNS.anomalyTuningKey();
    public static final String COST_KEY = ChartModifier.CHART_FORGE_COST_PERCENT.anomalyTuningKey();
    public static final String FORGE_TAG = ChartIds.PREFIX + "derelict_forge";
    public static final DerelictForge INSTANCE = new DerelictForge();
    static final String ENTITY_TYPE = ChartIds.PREFIX + "derelict_forge_station";
    public static final GuardedStation STATION = new GuardedStation(ENTITY_TYPE, FORGE_TAG, ChartIds.PREFIX + "derelict_forge_",
            "chart.anomaly.derelict_forge.name", ForgeGuards.GUARD_SALT);
    static final long PLACEMENT_SALT = 0x464f524745L;

    private DerelictForge() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        place(site);
    }

    static SectorEntityToken place(AnomalyContentSite site) {
        Random random = site.random(PLACEMENT_SALT);
        ChartSites.Orbit orbit = site.clearOrbit(OrbitSearch.bands(site.centre(), ForgeRules.ORBIT_MIN, ForgeRules.ORBIT_BAND_WIDTH, ForgeRules.ORBIT_BANDS),
                random);
        if (orbit == null) {
            throw new IllegalStateException("no clear orbit for the derelict forge");
        }
        String biomeId = site.context().biomeId();
        String infusionBiome = BiomeModifiers.hasModifiers(biomeId) ? biomeId : null;
        SectorEntityToken forge = STATION.place(site, orbit);
        ForgeStates.record(site.state(ANOMALY_ID), programs(infusionBiome != null), ForgeRules.runCount(site.context().anomalyPercent(EXTRA_RUNS_KEY)),
                infusionBiome);
        return forge;
    }

    static List<ForgePrograms.Program> programs(boolean biomeSystem) {
        ForgeOperation adding = biomeSystem ? ForgeOperation.BIOME_INFUSE : ForgeOperation.GRAFT;
        return Stream.of(adding, ForgeOperation.EXCISE, ForgeOperation.PERFECT).map(ForgePrograms::of).filter(Objects::nonNull).toList();
    }

    @Override
    public void loadData() {
        ForgePrograms.load();
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return STATION.holds(target) ? new ForgeDialog(ForgeSite.DERELICT_FORGE) : null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        int runsLeft = ForgeStates.runsLeft(run.run(), ForgeSite.DERELICT_FORGE);
        List<ForgePrograms.Program> programs = ForgeStates.programs(run.run(), ForgeSite.DERELICT_FORGE);
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
