package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;

import java.util.List;

public final class RemnantEncounter {

    static final String FOUGHT_REMNANT_KEY = "$exiledSector_foughtRemnant";
    private static final Logger LOG = Logger.getLogger(RemnantEncounter.class);

    private RemnantEncounter() {
    }

    public static boolean isFought(SectorAPI sector) {
        return sector != null && sector.getPlayerMemoryWithoutUpdate().getBoolean(FOUGHT_REMNANT_KEY);
    }

    public static void recordBattle(SectorAPI sector, BattleAPI battle) {
        if (sector == null || battle == null) {
            return;
        }
        boolean fought = isFought(sector);
        if (fought && !RelockedGates.isRelocked(sector, UnlockConditionType.REMNANT_FOUGHT)
                || !hasRemnantFleet(battle.getNonPlayerSideSnapshot())) {
            return;
        }
        RelockedGates.reopen(sector, UnlockConditionType.REMNANT_FOUGHT);
        if (!fought) {
            sector.getPlayerMemoryWithoutUpdate().set(FOUGHT_REMNANT_KEY, true);
            LOG.info("[ExiledSector] The player fought a Remnant fleet; the Remnant volume is now revealed");
        }
    }

    static boolean hasRemnantFleet(List<CampaignFleetAPI> fleets) {
        if (fleets == null) {
            return false;
        }
        for (CampaignFleetAPI fleet : fleets) {
            if (fleet != null && fleet.getFaction() != null && SkillTags.REDACTED_REGION.equals(NpcFactionVolumes.regionFor(fleet.getFaction().getId()))) {
                return true;
            }
        }
        return false;
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
