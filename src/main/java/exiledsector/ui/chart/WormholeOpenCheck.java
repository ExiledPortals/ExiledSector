package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.StarChartSettings;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChart;

import java.util.Locale;

public final class WormholeOpenCheck {

    public enum Blocker {
        NONE,
        NO_CHART,
        SETTING_OFF,
        RUN_OPEN,
        NO_FLEET,
        IN_TRANSITION,
        IN_POCKET,
        IN_FLEET_ENCOUNTER;

        public boolean allowsOpening() {
            return this == NONE;
        }

        public String reasonKey() {
            return "chart.ui.open.blocked." + name().toLowerCase(Locale.ROOT);
        }
    }

    private WormholeOpenCheck() {
    }

    public static Blocker current(SectorAPI sector, ItemStore store, Item chart) {
        return evaluate(chart, store, StarChartSettings.openAnywhere(), ChartRuns.hasRun(), sector == null ? null : sector.getPlayerFleet(),
                dialogTarget(sector));
    }

    static SectorEntityToken dialogTarget(SectorAPI sector) {
        CampaignUIAPI campaignUi = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = campaignUi == null ? null : campaignUi.getCurrentInteractionDialog();
        return dialog == null ? null : dialog.getInteractionTarget();
    }

    public static Blocker evaluate(Item chart, ItemStore store, boolean openAnywhere, boolean runOpen, CampaignFleetAPI playerFleet) {
        return evaluate(chart, store, openAnywhere, runOpen, playerFleet, null);
    }

    public static Blocker evaluate(Item chart, ItemStore store, boolean openAnywhere, boolean runOpen, CampaignFleetAPI playerFleet,
                                   SectorEntityToken dialogTarget) {
        if (!(chart instanceof StarChart) || chart.definition() == null || store == null || !store.holdsStarCharts()
                || store.find(chart.id()) != chart) {
            return Blocker.NO_CHART;
        }
        if (!openAnywhere) {
            return Blocker.SETTING_OFF;
        }
        if (runOpen) {
            return Blocker.RUN_OPEN;
        }
        LocationAPI location = playerFleet == null ? null : playerFleet.getContainingLocation();
        if (location == null) {
            return Blocker.NO_FLEET;
        }
        if (playerFleet.isInHyperspaceTransition()) {
            return Blocker.IN_TRANSITION;
        }
        if (location.hasTag(ChartIds.POCKET_TAG)) {
            return Blocker.IN_POCKET;
        }
        if (dialogTarget instanceof CampaignFleetAPI otherFleet && otherFleet != playerFleet) {
            return Blocker.IN_FLEET_ENCOUNTER;
        }
        return Blocker.NONE;
    }
}
