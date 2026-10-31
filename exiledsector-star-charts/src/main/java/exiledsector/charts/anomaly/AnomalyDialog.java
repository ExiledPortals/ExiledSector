package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import exiledsector.i18n.Translation;
import exiledsector.ui.BaseDialogPlugin;

public abstract class AnomalyDialog extends BaseDialogPlugin {

    public static final String LEAVE = "exiledSector_anomalyLeave";

    protected final void addLeaveOption() {
        addLeaveOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
    }

    protected final void continueAfter(ChartAnomaly self, SectorEntityToken site) {
        InteractionDialogPlugin next = ChartAnomalies.dialogAfter(self, site);
        handOff(next == null ? new RuleBasedInteractionDialogPluginImpl() : next);
    }
}
