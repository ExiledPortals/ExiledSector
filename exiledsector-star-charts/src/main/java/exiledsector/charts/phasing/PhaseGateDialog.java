package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.BaseDialogPlugin;
import exiledsector.ui.VanillaText;

public class PhaseGateDialog extends BaseDialogPlugin {

    static final String CANCEL = "exiledSector_phaseGateCancel";
    static final String LEAVE = "exiledSector_phaseGateLeave";

    private SectorEntityToken phaseGate;

    @Override
    protected void open() {
        phaseGate = dialog().getInteractionTarget();
        showEntityVisual(phaseGate);
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        MarketAPI market = PhasePull.gateMarketOf(phaseGate);
        PhasedPlanetRecord job = PhaseGateState.job(market);
        if (job == null) {
            VanillaText.plainPara(text, Translation.gameText("chart.phasing.gate.dialog.dormant"));
        } else {
            String colony = market.getName();
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.phasing.gate.dialog.description")
                    .arg("planet", job.name()).arg("colony", colony).text()));
            text.addPara("%s", Misc.getHighlightColor(), Misc.getHighlightColor(), PhasePull.statusLine(market));
            options.addOption(Translation.gameText("chart.phasing.gate.dialog.option.cancel"), CANCEL);
            options.addOptionConfirmation(CANCEL, I18n.forGameText(() -> Translation.msg("chart.phasing.gate.dialog.cancelConfirm")
                    .arg("planet", job.name()).text()), Translation.gameText("chart.phasing.gate.dialog.cancelYes"),
                    Translation.gameText("chart.phasing.gate.dialog.cancelNo"));
        }
        addLeaveOption(Translation.gameText("chart.wormhole.dialog.option.leave"), LEAVE);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!CANCEL.equals(optionData)) {
            dialog().dismiss();
            return;
        }
        MarketAPI market = PhasePull.gateMarketOf(phaseGate);
        PhasedPlanetRecord job = PhaseGateState.job(market);
        if (job != null && PhasePull.cancel(Global.getSector(), market)) {
            VanillaText.plainPara(text(), I18n.forGameText(() -> Translation.msg("chart.phasing.gate.dialog.cancelled")
                    .arg("planet", job.name()).text()));
        }
        options().clearOptions();
        addLeaveOption(Translation.gameText("chart.wormhole.dialog.option.leave"), LEAVE);
    }
}
