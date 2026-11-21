package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AbyssOrbits;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.GuardedStation;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartSites;

import java.util.List;

public final class AbyssalResearchComplex implements ContentPlacer, RunParticipant {

    public static final String ANOMALY_ID = "abyssal_research_complex";
    public static final String COMPLEX_TAG = ChartIds.PREFIX + "abyssal_research_complex";
    public static final AbyssalResearchComplex INSTANCE = new AbyssalResearchComplex();
    static final String ENTITY_TYPE = ChartIds.PREFIX + "abyssal_research_complex";
    public static final GuardedStation STATION = new GuardedStation(ENTITY_TYPE, COMPLEX_TAG, ChartIds.PREFIX + "abyssal_research_complex_",
            "chart.anomaly.abyssal_research_complex.name", ComplexGuards.GUARD_SALT);
    static final String BIOME_ID = "abyssal_rift";
    static final String INFUSE_PROGRAM = "infuse";
    static final int RUNS = 1;
    static final float MIN_ORBIT_GAP = 1500f;
    static final float MAX_ORBIT_GAP = 2500f;
    static final long PLACEMENT_SALT = 0x434f4d504c58L;

    private AbyssalResearchComplex() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        ChartSites.Orbit orbit = AbyssOrbits.nearPhotosphere(site, MIN_ORBIT_GAP, MAX_ORBIT_GAP, site.random(PLACEMENT_SALT));
        STATION.place(site, orbit);
        ForgePrograms.Program infuse = ForgePrograms.byId(INFUSE_PROGRAM);
        ForgeStates.record(site.state(ANOMALY_ID), infuse == null ? List.of() : List.of(infuse), RUNS, BIOME_ID);
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return ForgeSite.ABYSSAL_RESEARCH_COMPLEX.holds(target) && run.holds(target) ? new ForgeDialog(ForgeSite.ABYSSAL_RESEARCH_COMPLEX) : null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        SectorEntityToken complex = STATION.in(run.system());
        if (complex == null || ForgeStates.runsLeft(run.run(), ForgeSite.ABYSSAL_RESEARCH_COMPLEX) <= 0) {
            return null;
        }
        return Translation.gameText(VanillaDefenders.guarded(complex) ? "chart.anomaly.abyssal_research_complex.intel.guarded"
                : "chart.anomaly.abyssal_research_complex.intel.open");
    }
}
