package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

public final class OmegaEncounter {

    static final String FOUND_OMEGA_KEY = "$exiledSector_foundOmega";

    private OmegaEncounter() {
    }

    public static boolean isFound(SectorAPI sector) {
        MemoryAPI playerMemory = sector == null ? null : sector.getPlayerMemoryWithoutUpdate();
        return playerMemory != null && playerMemory.getBoolean(FOUND_OMEGA_KEY);
    }

    public static void recordBattle(SectorAPI sector, BattleAPI battle, boolean playerWon) {
        if (sector != null && playerWon && GateTriggers.awaits(sector, UnlockConditionType.OMEGA_FOUND, isFound(sector))
                && GateTriggers.foughtFaction(battle, Factions.OMEGA::equals)) {
            record(sector, "the player beat an Omega fleet");
        }
    }

    public static void recordCacheOpened(SectorAPI sector) {
        if (sector != null && GateTriggers.awaits(sector, UnlockConditionType.OMEGA_FOUND, isFound(sector))) {
            record(sector, "the player opened the shielded cache at Alpha Site");
        }
    }

    private static void record(SectorAPI sector, String reason) {
        GateTriggers.record(sector, UnlockConditionType.OMEGA_FOUND, sector.getPlayerMemoryWithoutUpdate(), FOUND_OMEGA_KEY,
                "Unlocking the Omega volume: " + reason);
    }

    public static final class BattleListener extends BaseCampaignEventListener {

        public BattleListener() {
            super(false);
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            if (result != null) {
                recordBattle(Global.getSector(), result.getBattle(), result.didPlayerWin());
            }
        }
    }
}
