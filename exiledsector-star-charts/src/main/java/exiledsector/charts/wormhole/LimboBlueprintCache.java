package exiledsector.charts.wormhole;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.items.JanusDevice;
import exiledsector.items.MaterialItems;
import exiledsector.items.WormholeTechItems;
import exiledsector.ui.CampaignMessages;
import org.apache.log4j.Logger;

public final class LimboBlueprintCache {

    public static final String PLACED_FLAG = "$exiledSector_blueprintPlaced";
    static final String CACHE_MEMORY_KEY = "$limboWormholeCache";
    static final String CALIBRATION_DATA_KEY = "$gotWormholeCalibrationData";
    static final String LIMBO_SYSTEM = "Limbo";
    private static final Logger LOG = Logger.getLogger(LimboBlueprintCache.class);

    public enum Outcome {
        ALREADY_SETTLED, PLACED, GRANTED, WAITING
    }

    private LimboBlueprintCache() {
    }

    public static void onGameLoad(SectorAPI sector) {
        sector.removeTransientScriptsOfClass(Watch.class);
        try {
            if (settle(sector) == Outcome.WAITING) {
                sector.addTransientScript(new Watch());
            }
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Placing the Wormhole Tech Blueprint failed", e);
        }
    }

    public static Outcome settle(SectorAPI sector) {
        MemoryAPI sectorMemory = sector.getMemoryWithoutUpdate();
        if (sectorMemory.getBoolean(PLACED_FLAG)) {
            return Outcome.ALREADY_SETTLED;
        }
        SectorEntityToken cache = findCache(sector);
        if (cache != null) {
            CargoAPI blueprint = Global.getFactory().createCargo(true);
            MaterialItems.add(blueprint, WormholeTechItems.BLUEPRINT, 1);
            BaseSalvageSpecial.addExtraSalvage(cache, blueprint);
            sectorMemory.set(PLACED_FLAG, true);
            return Outcome.PLACED;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || !calibrationDataTaken(sector) && !JanusDevice.obtained(sector)) {
            return Outcome.WAITING;
        }
        MaterialItems.add(playerFleet.getCargo(), WormholeTechItems.BLUEPRINT, 1);
        sectorMemory.set(PLACED_FLAG, true);
        CampaignMessages.plain(Translation.gameText("gatewayItem.blueprint.granted"), Misc.getPositiveHighlightColor());
        return Outcome.GRANTED;
    }

    static SectorEntityToken findCache(SectorAPI sector) {
        StarSystemAPI limbo = sector.getStarSystem(LIMBO_SYSTEM);
        if (limbo == null) {
            return null;
        }
        for (SectorEntityToken entity : limbo.getAllEntities()) {
            if (entity.getMemoryWithoutUpdate() != null && entity.getMemoryWithoutUpdate().getBoolean(CACHE_MEMORY_KEY) && !entity.isExpired()) {
                return entity;
            }
        }
        return null;
    }

    private static boolean calibrationDataTaken(SectorAPI sector) {
        MemoryAPI playerMemory = sector.getPlayerMemoryWithoutUpdate();
        return playerMemory != null && playerMemory.getBoolean(CALIBRATION_DATA_KEY);
    }

    static final class Watch implements EveryFrameScript {

        private final IntervalUtil interval = new IntervalUtil(1f, 1f);
        private boolean done;

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
            SectorAPI sector = Global.getSector();
            if (done || sector == null) {
                return;
            }
            interval.advance(sector.getClock().convertToDays(amount));
            if (!interval.intervalElapsed()) {
                return;
            }
            try {
                done = settle(sector) != Outcome.WAITING;
            } catch (RuntimeException e) {
                done = true;
                LOG.error("[ExiledSector] Granting the Wormhole Tech Blueprint failed", e);
            }
        }
    }
}
