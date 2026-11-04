package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.ui.socket.BenchMode;

public enum ForgeSite {
    DERELICT_FORGE(DerelictForge.ANOMALY_ID, DerelictForge.FORGE_TAG, BenchMode.FORGE, DerelictForge.COST_KEY) {
        @Override
        CampaignFleetAPI musterGuards(SectorEntityToken site, ChartRun run) {
            return ForgeGuards.muster(site, run);
        }
    },
    ABYSSAL_RESEARCH_COMPLEX(AbyssalResearchComplex.ANOMALY_ID, AbyssalResearchComplex.COMPLEX_TAG, BenchMode.ABYSSAL_FORGE, null) {
        @Override
        CampaignFleetAPI musterGuards(SectorEntityToken site, ChartRun run) {
            return ComplexGuards.muster(site, run);
        }
    };

    private final String anomalyId;
    private final String tag;
    private final BenchMode benchMode;
    private final String costKey;

    ForgeSite(String anomalyId, String tag, BenchMode benchMode, String costKey) {
        this.anomalyId = anomalyId;
        this.tag = tag;
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
        return entity != null && entity.hasTag(tag);
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
