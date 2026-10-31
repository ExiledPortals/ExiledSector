package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.ui.VanillaText;

public class ConvoyEncounterDialog extends AnomalyDialog {

    static final String ATTACK = "exiledSector_convoyAttack";

    private CampaignFleetAPI fleet;

    @Override
    protected void open() {
        fleet = (CampaignFleetAPI) dialog().getInteractionTarget();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        dialog().getVisualPanel().showFleetInfo(null, playerFleet, null, fleet);
        ConvoyScript script = ConvoyScript.in(fleet.getContainingLocation());
        String fleetName = fleet.getName();
        VanillaText.plainPara(text(), I18n.forGameText(() -> Translation.msg("chart.convoy.encounter.description")
                .arg("fleet", fleetName).text()));
        if (script != null) {
            int tier = script.tier();
            VanillaText.addPara(text(), I18n.forGameText(() -> Translation.msg("chart.convoy.intel.manifest")
                    .arg("cargo", ConvoyCargo.manifest(tier)).styled()), Misc.getTextColor());
        }
        OptionPanelAPI options = options();
        options.clearOptions();
        options.addOption(Translation.gameText("chart.convoy.encounter.option.attack"), ATTACK);
        options.addOptionConfirmation(ATTACK, Translation.gameText("chart.convoy.encounter.confirm"),
                Translation.gameText("chart.convoy.encounter.yes"), Translation.gameText("chart.convoy.encounter.no"));
        addLeaveOption();
    }

    static boolean wantsToHandle(Object target) {
        return target instanceof CampaignFleetAPI convoyFleet && ConvoyHostility.isPeaceful(convoyFleet) && ConvoyScript.convoyOf(convoyFleet) != null;
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!ATTACK.equals(optionData)) {
            dialog().dismiss();
            return;
        }
        ConvoyScript script = ConvoyScript.in(fleet.getContainingLocation());
        ConvoyState convoy = ConvoyScript.convoyOf(fleet);
        if (script != null && convoy != null) {
            script.turnHostile(convoy);
        }
        handOff(new FleetInteractionDialogPluginImpl());
    }
}
