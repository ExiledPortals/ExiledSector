package exiledsector.loot;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CargoGainedListener;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;

import java.util.function.Supplier;

public final class LootScreenTrigger implements ShowLootListener, CargoGainedListener {

    static final int MAX_UNOPENED_FRAMES = 60;

    private ClosingWatch activeWatch;

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI loot, InteractionDialogAPI dialog) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }
        if (activeWatch != null) {
            activeWatch.done = true;
        }
        activeWatch = new ClosingWatch(LootScreenTrigger::currentCoreTab, LootTaken::report);
        sector.addTransientScript(activeWatch);
    }

    @Override
    public void reportSpecialCargoGainedFromRecoveredDerelict(CargoAPI loot, InteractionDialogAPI dialog) {
        LootTaken.report();
    }

    private static CoreUITabId currentCoreTab() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        return campaignUI == null ? null : campaignUI.getCurrentCoreTab();
    }

    static final class ClosingWatch implements EveryFrameScript {

        private final Supplier<CoreUITabId> currentCoreTab;
        private final Runnable onClosed;
        private boolean opened;
        private int unopenedFrames;
        private boolean done;

        ClosingWatch(Supplier<CoreUITabId> currentCoreTab, Runnable onClosed) {
            this.currentCoreTab = currentCoreTab;
            this.onClosed = onClosed;
        }

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
            if (done) {
                return;
            }
            if (currentCoreTab.get() != null) {
                opened = true;
            } else if (opened) {
                done = true;
                onClosed.run();
            } else if (++unopenedFrames > MAX_UNOPENED_FRAMES) {
                done = true;
            }
        }
    }
}
