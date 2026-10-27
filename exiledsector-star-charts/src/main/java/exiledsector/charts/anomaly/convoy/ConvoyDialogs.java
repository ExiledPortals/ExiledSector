package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public final class ConvoyDialogs {

    private ConvoyDialogs() {
    }

    public static InteractionDialogPlugin pick(SectorEntityToken target) {
        if (ConvoyEncounterDialog.wantsToHandle(target)) {
            return new ConvoyEncounterDialog();
        }
        if (ConvoyCacheDialog.wantsToHandle(target)) {
            return new ConvoyCacheDialog();
        }
        return null;
    }
}
