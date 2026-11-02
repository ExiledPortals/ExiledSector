package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import exiledsector.PlayerFleet;
import exiledsector.i18n.Translation;

final class ExcavationEncounterDialog extends FleetInteractionDialogPluginImpl {

    ExcavationEncounterDialog(FIDConfig config) {
        super(config);
    }

    @Override
    protected void updateEngagementChoice(boolean withText) {
        super.updateEngagementChoice(withText);
        disableEngageWithoutExcavationShips(options, PlayerFleet.orNull());
    }

    static void disableEngageWithoutExcavationShips(OptionPanelAPI options, CampaignFleetAPI playerFleet) {
        if (options != null && options.hasOption(OptionId.ENGAGE) && !ExcavationSiteDialog.hasExcavationShips(playerFleet)) {
            options.setEnabled(OptionId.ENGAGE, false);
            options.setTooltip(OptionId.ENGAGE, Translation.gameText("chart.anomaly.excavation_sites.dialog.noShaftShips"));
        }
    }
}
