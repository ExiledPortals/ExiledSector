package exiledsector.ui.cargo;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;
import com.fs.starfarer.api.campaign.listeners.SubmarketUpdateListener;

public final class LootAlertListener implements ShowLootListener, SubmarketUpdateListener {

    static final String LOOT_MARKET_ID = "fake_market";

    private static Watcher activeWatcher;

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI loot, InteractionDialogAPI dialog) {
        if (activeWatcher != null) {
            activeWatcher.done = true;
            activeWatcher = null;
        }
        SectorAPI sector = Global.getSector();
        if (loot == null || sector == null) {
            return;
        }
        LootAlertCascade cascade = LootAlertCascade.of(loot.getStacksCopy(), LootAlert::of, System.nanoTime());
        if (cascade.hasAlerts()) {
            activeWatcher = new Watcher(loot, cascade);
            sector.addTransientScript(activeWatcher);
        }
    }

    @Override
    public void reportSubmarketCargoAndShipsUpdated(SubmarketAPI submarket) {
        MarketAPI market = submarket == null ? null : submarket.getMarket();
        if (activeWatcher != null && !activeWatcher.started && market != null && LOOT_MARKET_ID.equals(market.getId())) {
            activeWatcher.shownCargo = submarket.getCargo();
        }
    }

    static Watcher activeWatcher() {
        return activeWatcher;
    }

    static void resetForTests() {
        activeWatcher = null;
    }

    static final class Watcher implements EveryFrameScript {

        private final CargoAPI loot;
        private final LootAlertCascade cascade;
        private CargoAPI shownCargo;
        private boolean started;
        private boolean done;

        Watcher(CargoAPI loot, LootAlertCascade cascade) {
            this.loot = loot;
            this.cascade = cascade;
        }

        CargoAPI watchedCargo() {
            return shownCargo != null ? shownCargo : loot;
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
            started = true;
            long nowNanos = System.nanoTime();
            cascade.advance(watchedCargo().getStacksCopy(), nowNanos, Watcher::play);
            done = cascade.finished(nowNanos);
        }

        private static void play(String soundId) {
            SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
            if (soundPlayer != null) {
                soundPlayer.playUISound(soundId, 1f, 1f);
            }
        }
    }
}
