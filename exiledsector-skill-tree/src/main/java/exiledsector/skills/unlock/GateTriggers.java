package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.function.Predicate;

public final class GateTriggers {

    private static final Logger LOG = Logger.getLogger(GateTriggers.class);

    private GateTriggers() {
    }

    public static boolean awaits(SectorAPI sector, UnlockConditionType conditionType, boolean alreadyMet) {
        return !alreadyMet || RelockedGates.isRelocked(sector, conditionType);
    }

    public static void record(SectorAPI sector, UnlockConditionType conditionType, MemoryAPI flagMemory, String flagKey, String reason) {
        RelockedGates.reopen(sector, conditionType);
        if (flagMemory != null && flagKey != null && !flagMemory.getBoolean(flagKey)) {
            flagMemory.set(flagKey, true);
            LOG.info("[ExiledSector] " + reason + ".");
        }
    }

    public static boolean foughtFaction(BattleAPI battle, Predicate<String> factionMatches) {
        List<CampaignFleetAPI> fleets = battle == null ? null : battle.getNonPlayerSideSnapshot();
        if (fleets == null) {
            return false;
        }
        for (CampaignFleetAPI fleet : fleets) {
            if (fleet != null && fleet.getFaction() != null && factionMatches.test(fleet.getFaction().getId())) {
                return true;
            }
        }
        return false;
    }
}
