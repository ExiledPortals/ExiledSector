package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;


public final class RemnantEncounter {

    static final String FOUGHT_REMNANT_KEY = "$exiledSector_foughtRemnant";

    private RemnantEncounter() {
    }

    public static boolean isFought(SectorAPI sector) {
        return sector != null && sector.getPlayerMemoryWithoutUpdate().getBoolean(FOUGHT_REMNANT_KEY);
    }

    public static void recordBattle(SectorAPI sector, BattleAPI battle) {
        if (sector == null || battle == null) {
            return;
        }
        if (GateTriggers.awaits(sector, UnlockConditionType.REMNANT_FOUGHT, isFought(sector))
                && GateTriggers.foughtFaction(battle, factionId -> SkillTags.REDACTED_REGION.equals(NpcFactionVolumes.regionFor(factionId)))) {
            GateTriggers.record(sector, UnlockConditionType.REMNANT_FOUGHT, sector.getPlayerMemoryWithoutUpdate(), FOUGHT_REMNANT_KEY,
                    "The player fought a Remnant fleet; the Remnant volume is now revealed");
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
