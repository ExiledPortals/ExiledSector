package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import exiledsector.i18n.Translation;

final class ShaftEncounterDialog extends FleetInteractionDialogPluginImpl {

    ShaftEncounterDialog(FIDConfig config) {
        super(config);
    }

    @Override
    protected void updateEngagementChoice(boolean withText) {
        super.updateEngagementChoice(withText);
        disableEngageWithoutShaftShips(options, Global.getSector().getPlayerFleet());
    }

    static void disableEngageWithoutShaftShips(OptionPanelAPI options, CampaignFleetAPI playerFleet) {
        if (options != null && options.hasOption(OptionId.ENGAGE) && !ExcavationSiteDialog.hasShaftShips(playerFleet)) {
            options.setEnabled(OptionId.ENGAGE, false);
            options.setTooltip(OptionId.ENGAGE, Translation.gameText("chart.excavation.dialog.noShaftShips"));
        }
    }
}
