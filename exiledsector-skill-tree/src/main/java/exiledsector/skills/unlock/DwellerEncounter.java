package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

public final class DwellerEncounter {

    static final String ENCOUNTERED_DWELLER_KEY = "$encounteredDweller";

    private DwellerEncounter() {
    }

    public static boolean isFought(SectorAPI sector) {
        return sector != null && sector.getPlayerMemoryWithoutUpdate().getBoolean(ENCOUNTERED_DWELLER_KEY);
    }

    public static void recordBattle(SectorAPI sector, BattleAPI battle) {
        if (sector != null && GateTriggers.awaits(sector, UnlockConditionType.DWELLER_FOUGHT, isFought(sector))
                && GateTriggers.foughtFaction(battle, Factions.DWELLER::equals)) {
            GateTriggers.record(sector, UnlockConditionType.DWELLER_FOUGHT, null, null, null);
        }
    }

    public static final class BattleListener extends BaseCampaignEventListener {

        public BattleListener() {
            super(false);
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            recordBattle(Global.getSector(), result == null ? null : result.getBattle());
        }
    }
}
