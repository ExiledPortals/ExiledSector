package exiledsector.charts.wormhole;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRunActions.OpenResult;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChart;

import java.util.Locale;

public final class WormholeOpenCheck {

    public enum Blocker {
        NONE,
        DISABLED,
        NO_CHART,
        NO_FLEET,
        IN_TRANSITION,
        IN_SYSTEM,
        IN_FLEET_ENCOUNTER,
        NO_GATEWAY,
        GATEWAY_INACTIVE;

        public boolean allowsOpening() {
            return this == NONE;
        }

        public String reasonKey() {
            return "chart.ui.open.blocked." + name().toLowerCase(Locale.ROOT);
        }
    }

    private WormholeOpenCheck() {
    }

    public static Blocker replacingRun(SectorAPI sector, ItemStore store, Item chart) {
        return evaluate(chart, store, StarChartSettings.openAnywhere(), sector == null ? null : sector.getPlayerFleet(), dialogTarget(sector));
    }

    static SectorEntityToken dialogTarget(SectorAPI sector) {
        CampaignUIAPI campaignUi = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = campaignUi == null ? null : campaignUi.getCurrentInteractionDialog();
        return dialog == null ? null : dialog.getInteractionTarget();
    }

    public static Blocker evaluate(Item chart, ItemStore store, boolean openAnywhere, CampaignFleetAPI playerFleet) {
        return evaluate(chart, store, openAnywhere, playerFleet, null);
    }

    public static Blocker evaluate(Item chart, ItemStore store, boolean openAnywhere, CampaignFleetAPI playerFleet, SectorEntityToken dialogTarget) {
        if (!StarChartSettings.enabled()) {
            return Blocker.DISABLED;
        }
        if (!(chart instanceof StarChart) || chart.definition() == null || store == null || !store.holdsStarCharts()
                || store.find(chart.id()) != chart) {
            return Blocker.NO_CHART;
        }
        OpenResult fleetBlocker = ChartRunActions.fleetBlocker(playerFleet);
        if (fleetBlocker != null) {
            return switch (fleetBlocker) {
                case PLAYER_IN_TRANSITION -> Blocker.IN_TRANSITION;
                case PLAYER_IN_SYSTEM -> Blocker.IN_SYSTEM;
                default -> Blocker.NO_FLEET;
            };
        }
        if (dialogTarget instanceof CampaignFleetAPI otherFleet && otherFleet != playerFleet) {
            return Blocker.IN_FLEET_ENCOUNTER;
        }
        if (openAnywhere) {
            return Blocker.NONE;
        }
        return switch (WormholeGateway.statusAt(dialogTarget)) {
            case WORKING -> Blocker.NONE;
            case INACTIVE -> Blocker.GATEWAY_INACTIVE;
            case NONE -> Blocker.NO_GATEWAY;
        };
    }
}
