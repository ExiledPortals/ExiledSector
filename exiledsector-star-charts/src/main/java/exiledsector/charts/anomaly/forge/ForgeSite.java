package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.GuardedStation;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.ui.socket.BenchMode;

public enum ForgeSite {
    DERELICT_FORGE(DerelictForge.ANOMALY_ID, DerelictForge.STATION, BenchMode.FORGE, DerelictForge.COST_KEY) {
        @Override
        CampaignFleetAPI musterGuards(SectorEntityToken site, ChartRun run) {
            return ForgeGuards.muster(site, run);
        }
    },
    ABYSSAL_RESEARCH_COMPLEX(AbyssalResearchComplex.ANOMALY_ID, AbyssalResearchComplex.STATION, BenchMode.ABYSSAL_FORGE, null) {
        @Override
        CampaignFleetAPI musterGuards(SectorEntityToken site, ChartRun run) {
            return ComplexGuards.muster(site, run);
        }
    };

    private final String anomalyId;
    private final GuardedStation station;
    private final BenchMode benchMode;
    private final String costKey;

    ForgeSite(String anomalyId, GuardedStation station, BenchMode benchMode, String costKey) {
        this.anomalyId = anomalyId;
        this.station = station;
        this.benchMode = benchMode;
        this.costKey = costKey;
    }

    abstract CampaignFleetAPI musterGuards(SectorEntityToken site, ChartRun run);

    public String anomalyId() {
        return anomalyId;
    }

    public BenchMode benchMode() {
        return benchMode;
    }

    public boolean holds(SectorEntityToken entity) {
        return station.holds(entity);
    }

    ChartRun liveRunFor(SectorEntityToken entity) {
        return station.liveRun(entity, anomalyId);
    }

    public String textKey(String suffix) {
        return "chart.anomaly." + anomalyId + "." + suffix;
    }

    float costMult(ChartRun run) {
        return costKey == null ? 1f : ChartRunEffects.contextOf(run).anomalyMult(costKey);
    }

    public static ForgeSite of(SectorEntityToken entity) {
        for (ForgeSite site : values()) {
            if (site.holds(entity)) {
                return site;
            }
        }
        return null;
    }
}
