package exiledsector.ui.cargo;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.ui.UIPanelAPI;

public final class CargoCraftingTabListener implements CoreUITabListener {

    static final int MAX_WAIT_FRAMES = 60;

    private Watcher activeWatcher;

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if (tab != CoreUITabId.CARGO || CargoCraftingButton.failed()) return;
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        if (activeWatcher != null) {
            activeWatcher.done = true;
        }
        activeWatcher = new Watcher();
        sector.addTransientScript(activeWatcher);
    }

    static final class Watcher implements EveryFrameScript {

        private UIPanelAPI tradePanel;
        private int unresolvedFrames;
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
            if (done) return;
            SectorAPI sector = Global.getSector();
            CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
            if (campaignUI == null || CargoCraftingButton.failed()) {
                done = true;
                return;
            }
            if (campaignUI.getCurrentCoreTab() != CoreUITabId.CARGO) {
                if (tradePanel != null || waitedTooLong()) done = true;
                return;
            }
            if (tradePanel == null) {
                tradePanel = CargoCraftingButton.tradePanel(campaignUI);
                if (tradePanel == null) {
                    if (waitedTooLong()) done = true;
                    return;
                }
            }
            CargoCraftingButton.attachIfMissing(tradePanel);
        }

        private boolean waitedTooLong() {
            unresolvedFrames++;
            return unresolvedFrames > MAX_WAIT_FRAMES;
        }
    }
}
