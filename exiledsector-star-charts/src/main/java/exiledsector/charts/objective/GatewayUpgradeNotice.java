package exiledsector.charts.objective;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.List;

final class GatewayUpgradeNotice implements EveryFrameScript {

    private static final float CHECK_SECONDS = 0.5f;

    private float sinceCheck;
    private boolean done;

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        sinceCheck += amount;
        if (done || sinceCheck < CHECK_SECONDS) {
            return;
        }
        sinceCheck = 0f;
        SectorAPI sector = Global.getSector();
        if (sector == null || !canShow(sector)) {
            return;
        }
        List<String> pending = GatewayUpgradeRewards.pending(sector);
        if (pending.isEmpty()) {
            done = true;
            return;
        }
        if (sector.getCampaignUI().showInteractionDialog(new GatewayUpgradeDialog(pending), sector.getPlayerFleet())) {
            GatewayUpgradeRewards.clearPending(sector);
            done = true;
        }
    }

    static boolean canShow(SectorAPI sector) {
        CampaignUIAPI campaignUi = sector.getCampaignUI();
        return Global.getCurrentState() == GameState.CAMPAIGN && campaignUi != null && sector.getPlayerFleet() != null
                && !sector.isInFastAdvance() && !sector.isInNewGameAdvance() && !campaignUi.isShowingDialog() && !campaignUi.isShowingMenu()
                && campaignUi.getCurrentCoreTab() == null;
    }
}
