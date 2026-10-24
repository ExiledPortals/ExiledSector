package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

public final class ThreatEncounter {

    static final String ENCOUNTERED_THREAT_KEY = "$encounteredThreat";

    private ThreatEncounter() {
    }

    public static boolean isFought(SectorAPI sector) {
        return sector != null && sector.getPlayerMemoryWithoutUpdate().getBoolean(ENCOUNTERED_THREAT_KEY);
    }

    public static void recordBattle(SectorAPI sector, BattleAPI battle) {
        if (sector != null && GateTriggers.awaits(sector, UnlockConditionType.THREAT_FOUGHT, isFought(sector))
                && GateTriggers.foughtFaction(battle, Factions.THREAT::equals)) {
            GateTriggers.record(sector, UnlockConditionType.THREAT_FOUGHT, null, null, null);
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
