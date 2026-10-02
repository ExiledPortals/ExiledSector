package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.LiveMunitionsCrew;
import exiledsector.ui.VanillaText;

public class LiveMunitionsCrewListener extends BaseCampaignEventListener {

    public LiveMunitionsCrewListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        int deaths = LiveMunitionsCrew.drainPendingDeaths();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (deaths <= 0 || playerFleet == null) {
            return;
        }
        CargoAPI cargo = playerFleet.getCargo();
        int lost = Math.min(deaths, (int) cargo.getCrew());
        if (lost <= 0) {
            return;
        }
        cargo.removeCrew(lost);
        I18n.forGameText(() -> report(lost));
    }

    private static void report(int lost) {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null || dialog.getTextPanel() == null) {
            return;
        }
        VanillaText.addPara(dialog.getTextPanel(), Translation.msg("combat.liveMunitions.crewLost").count(lost).arg("count", lost).styled(),
                Misc.getNegativeHighlightColor());
    }
}
