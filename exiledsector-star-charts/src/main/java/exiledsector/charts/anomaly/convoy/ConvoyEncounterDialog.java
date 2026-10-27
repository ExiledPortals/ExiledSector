package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class ConvoyEncounterDialog implements InteractionDialogPlugin {

    static final String ATTACK = "exiledSector_convoyAttack";
    static final String LEAVE = "exiledSector_convoyLeave";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;
    private CampaignFleetAPI fleet;

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        fleet = (CampaignFleetAPI) interactionDialog.getInteractionTarget();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        dialog.getVisualPanel().showFleetInfo(null, playerFleet, null, fleet);
        ConvoyScript script = ConvoyScript.in(fleet.getContainingLocation());
        String fleetName = fleet.getName();
        VanillaText.plainPara(dialog.getTextPanel(), I18n.forGameText(() -> Translation.msg("chart.convoy.encounter.description")
                .arg("fleet", fleetName).text()));
        if (script != null) {
            int tier = script.tier();
            VanillaText.addPara(dialog.getTextPanel(), I18n.forGameText(() -> Translation.msg("chart.convoy.intel.manifest")
                    .arg("cargo", ConvoyCargo.manifest(tier)).styled()), Misc.getTextColor());
        }
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        options.addOption(Translation.gameText("chart.convoy.encounter.option.attack"), ATTACK);
        options.addOptionConfirmation(ATTACK, Translation.gameText("chart.convoy.encounter.confirm"),
                Translation.gameText("chart.convoy.encounter.yes"), Translation.gameText("chart.convoy.encounter.no"));
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    static boolean wantsToHandle(Object target) {
        return target instanceof CampaignFleetAPI convoyFleet && ConvoyHostility.isPeaceful(convoyFleet) && ConvoyScript.convoyOf(convoyFleet) != null;
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!ATTACK.equals(optionData)) {
            dialog.dismiss();
            return;
        }
        ConvoyScript script = ConvoyScript.in(fleet.getContainingLocation());
        ConvoyState convoy = ConvoyScript.convoyOf(fleet);
        if (script != null && convoy != null) {
            script.turnHostile(convoy);
        }
        FleetInteractionDialogPluginImpl encounter = new FleetInteractionDialogPluginImpl();
        dialog.setPlugin(encounter);
        encounter.init(dialog);
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }
}
