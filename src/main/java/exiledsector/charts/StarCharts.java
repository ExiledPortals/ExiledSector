package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorAPI;

public final class StarCharts {

    private StarCharts() {
    }

    public static void onGameLoad(SectorAPI sector) {
        ChartLoadSweep.sweep(sector);
        sector.unregisterPlugin(ChartCampaignPlugin.ID);
        sector.registerPlugin(new ChartCampaignPlugin());
        sector.removeTransientScriptsOfClass(ChartRunScript.class);
        sector.removeTransientScriptsOfClass(ChartSoakScript.class);
        sector.addTransientScript(new ChartRunScript());
    }
}
